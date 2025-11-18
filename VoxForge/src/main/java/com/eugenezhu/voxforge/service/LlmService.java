package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.config.AiConfig;
import com.eugenezhu.voxforge.model.CommandTemplate;
import com.eugenezhu.voxforge.model.LlmRequest;
import com.eugenezhu.voxforge.model.LlmResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.service
 * @className: LlmService
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/21 下午8:29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {
    @Qualifier("llmWebClient")
    private final WebClient llmWebClient;
    private final AiConfig.LlmProperties llmProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Qualifier("llmRateLimiter")
    private final RateLimiter llmRateLimiter;

    @Qualifier("llmBulkhead")
    private final Bulkhead llmBulkhead;

    @Qualifier("llmCircuitBreaker")
    private final CircuitBreaker llmCircuitBreaker;

    private final ResilienceTuner resilienceTuner;
    private final RagService ragService;

    public Mono<LlmResponse> parseUserInput(String text, Map<String, Object> clientEnv) {
        log.info("正在解析用户输入：{}", text);

        return ragService.retrieve(text, clientEnv, 5)
                .defaultIfEmpty(List.of())
                .flatMap(candidates -> {
                    String prompt = buildStartPromptRag(clientEnv, candidates);
                    LlmRequest request = new LlmRequest(prompt, text, llmProperties.getModel());

                    long startTime = System.nanoTime();

                    if (!llmCircuitBreaker.tryAcquirePermission()) {
                        resilienceTuner.recordRejection("llm");
                        return Mono.error(new RuntimeException("llmCircuitBreaker 拒绝请求"));
                    }

                    if (!llmBulkhead.tryAcquirePermission()) {
                        resilienceTuner.recordRejection("llm");
                        return Mono.error(new RuntimeException("llmBulkhead 拒绝请求"));
                    }

                    if (!llmRateLimiter.acquirePermission()) {
                        resilienceTuner.recordRejection("llm");
                        return Mono.error(new RuntimeException("llmRateLimiter 拒绝请求"));
                    }

                    resilienceTuner.recordCall("llm");

                    return llmWebClient
                            .post()
                            .uri("/chat/completions")
                            .header("Authorization", "Bearer " + llmProperties.getApiKey())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(request))
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::parseLlmResponse)
                            .retryWhen(
                                    Retry.backoff(5, Duration.ofSeconds(3))
                                            .maxBackoff(Duration.ofSeconds(10))
                                            .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                            )
                            .doOnSuccess(response -> {
                                llmCircuitBreaker.onSuccess(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                                int taskCount = response.getTasks() != null ? response.getTasks().size() : 0;
                                log.info("成功解析用户输入, 生成 {} 个任务", taskCount);
                            })
                            .doOnError(error -> {
                                llmCircuitBreaker.onError(System.nanoTime() - startTime, TimeUnit.NANOSECONDS, error);
                                log.error("解析用户输入失败：{}", error.getMessage(), error);
                            })
                            .doFinally(signalType -> llmBulkhead.releasePermission())
                            .onErrorReturn(createErrorResponse("LLM 服务暂时不可用，请稍后重试"));
                });
    }

    public Mono<String> generateNextStep(String taskTitle, String feedback, java.util.Map<String, Object> clientEnv) {
        log.info("生成下一步指示，任务: {}, 反馈: {}", taskTitle, feedback);

        String prompt = buildNextPrompt(taskTitle, feedback, clientEnv);

        LlmRequest request = new LlmRequest("你是一个智能助手，负责指导用户完成任务。", prompt, llmProperties.getModel());

        return llmWebClient
                .post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + llmProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTextFromResponse)
                .doOnSuccess(guidance -> log.info("生成下一步指示: {}", guidance))
                .doOnError(error -> log.error("生成下一步指示失败", error))
                .onErrorReturn("请继续执行下一个任务");
    }

    private LlmResponse parseLlmResponse(String responseJson) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");

                // 清理可能的非JSON内容
                String cleanedContent = cleanJsonContent(content);

                // 解析JSON格式的任务内容
                return objectMapper.readValue(cleanedContent, LlmResponse.class);
            }

            return createErrorResponse("LLM响应格式错误");
        } catch (JsonProcessingException e) {
            log.error("解析LLM响应失败", e);
            return createErrorResponse("解析LLM响应失败");
        }
    }

    private String cleanJsonContent(String content) {
        if (content == null) {
            return "{}";
        }

        // 去除前后空白字符
        content = content.trim();

        // 查找第一个 { 和最后一个 }
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            // 提取JSON部分
            return content.substring(firstBrace, lastBrace + 1);
        }

        // 如果没有找到有效的JSON结构，返回空的响应
        log.warn("无法从LLM响应中提取有效JSON: {}", content);
        return "{\"reply\":\"解析失败\",\"tasks\":[]}";
    }

    private String extractTextFromResponse(String responseJson) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            return "处理完成";
        } catch (JsonProcessingException e) {
            log.error("提取文本内容失败", e);
            return "处理完成";
        }
    }

    private LlmResponse createErrorResponse(String errorMessage) {
        LlmResponse response = new LlmResponse();
        response.setReply(errorMessage);
        response.setTasks(List.of());
        return response;
    }

    private String buildStartPrompt(java.util.Map<String, Object> clientEnv) {
        String clientEnvStr = "";
        if (clientEnv != null) {
            try {
                clientEnvStr = objectMapper.writeValueAsString(clientEnv);
            } catch (Exception e) {
                log.warn("序列化客户端环境信息失败", e);
                clientEnvStr = clientEnv.toString();
            }
        }

        return String.format("""
                你是一个智能语音助手，专门帮助用户完成各种任务。
                
                用户的客户端环境信息：
                %s
                
                请根据用户的输入，分析用户的意图，并生成一个详细的任务链来帮助用户完成目标。
                
                只返回如下JSON：
                {
                  "reply": "",
                  "tasks": [
                    {
                      "title": "",
                      "cmd": "",
                      "shell": "cmd|powershell|bash",
                      "os": "Windows 11|Ubuntu 22.04"
                    }
                  ]
                }
                """,
                clientEnvStr.isEmpty() ? "无详细客户端环境信息, 默认为Windows环境" : clientEnvStr
        );
    }

    private String buildStartPromptRag(Map<String, Object> clientEnv, List<CommandTemplate> candidates) {
        String clientEnvStr = "";
        if (clientEnv != null) {
            try {
                clientEnvStr = objectMapper.writeValueAsString(clientEnv);
            } catch (Exception e) {
                clientEnvStr = clientEnv.toString();
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(5, candidates.size()); i++) {
            com.eugenezhu.voxforge.model.CommandTemplate c = candidates.get(i);
            sb.append(i + 1).append('.').append(' ').append(c.getCmd()).append(' ').append('-').append(' ').append(c.getDesc()).append('\n');
        }
        String hint = sb.toString();
        return String.format("""
                你是一个智能语音助手，专门帮助用户完成任务。
                用户的客户端环境信息：
                %s
                参考候选命令：
                %s
                生成JSON任务链，优先提供结合环境信息修改后的候选命令。
                {
                  "reply": "",
                  "tasks": [
                    {"title": "","cmd": "","shell": "cmd|powershell|bash","os": "Windows 11|Ubuntu 22.04"}
                  ]
                }
                """,
                clientEnvStr.isEmpty() ? "无详细客户端环境信息" : clientEnvStr,
                hint
        );
    }

    private String buildNextPrompt(String taskTitle, String feedback, java.util.Map<String, Object> clientEnv) {
        String clientEnvStr = "";
        if (clientEnv != null) {
            try {
                clientEnvStr = objectMapper.writeValueAsString(clientEnv);
            } catch (Exception e) {
                log.warn("序列化客户端环境信息失败", e);
                clientEnvStr = clientEnv.toString();
            }
        }

        return String.format("""
                当前任务：%s
                用户反馈：%s
                
                用户的客户端环境信息：
                %s
                
                请根据用户的反馈，生成下一步的指导建议。
                如果任务已完成，请给出总结。
                如果遇到问题，请提供解决方案。
                """,
                taskTitle,
                feedback,
                clientEnvStr.isEmpty() ? "无详细客户端环境信息" : clientEnvStr
        );
    }
}

