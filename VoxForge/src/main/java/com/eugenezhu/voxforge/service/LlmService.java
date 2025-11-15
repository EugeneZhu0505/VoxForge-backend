package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.config.AiConfig;
import com.eugenezhu.voxforge.model.LlmRequest;
import com.eugenezhu.voxforge.model.LlmResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public Mono<LlmResponse> parseUserInput(String text, java.util.Map<String, Object> clientEnv) {
        log.info("正在解析用户输入：{}", text);

        String prompt = buildStartPrompt(clientEnv);

        LlmRequest request = new LlmRequest(prompt, text, llmProperties.getModel());

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
                .doOnSuccess(response -> log.info("成功解析用户输入, 生成 {} 个任务", response.getTasks().size()))
                .doOnError(error -> log.error("解析用户输入失败：{}", error.getMessage(), error))
                .onErrorReturn(createErrorResponse("LLM 服务暂时不可用，请稍后重试"));
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
                
                **重要：你必须严格按照以下JSON格式返回，不要添加任何解释文字或其他内容，只返回纯JSON：**
                
                {
                  "reply": "对用户的回复消息",
                  "tasks": [
                    {
                      "title": "任务标题",
                      "cmd": "具体的命令或操作"
                    }
                  ]
                }
                
                **注意事项：**
                1. reply字段：包含对用户的友好回复
                2. tasks数组：包含具体的任务列表
                3. title：任务的简短描述
                4. cmd：直接在终端运行即可的命令(无需添加任何前缀或后缀, 如bash、sh等), 客户端接收后直接执行
                
                **严格要求：只返回JSON，不要有任何其他文字！**
                """,
                clientEnvStr.isEmpty() ? "无详细客户端环境信息, 默认为Windows环境, 此时启动软件等命令不要给带有路径的, 如C:\\Program Files\\Google\\Chrome\\chrome.exe" : clientEnvStr
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

