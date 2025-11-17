package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.config.AsrTtsConfig;
import com.eugenezhu.voxforge.model.TtsRequest;
import com.eugenezhu.voxforge.model.TtsResponse;
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
import java.util.concurrent.TimeUnit;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.service
 * @className: TtsService
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/21 下午8:05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {
    @Qualifier("ttsWebClient")
    private final WebClient ttsWebClient;
    private final AsrTtsConfig.TtsProperties ttsProperties;
    // 注入 OSS 服务用于上传音频文件（暂时模拟）
    private final OssService ossService;

    @Qualifier("ttsCircuitBreaker")
    private final CircuitBreaker ttsCircuitBreaker;

    @Qualifier("ttsBulkhead")
    private final Bulkhead ttsBulkhead;

    @Qualifier("ttsRateLimiter")
    private final RateLimiter ttsRateLimiter;

    private final ResilienceTuner resilienceTuner;

    public Mono<String> textToSpeech(String text, String voiceType, Float speedRatio) {
        log.info("开始文本转语音，文本：{}，语音类型：{}，语速：{}", text, voiceType, speedRatio);

        TtsRequest request = new TtsRequest(text, voiceType, speedRatio);
        
        // 添加调试日志，打印实际发送的JSON
        log.debug("发送TTS请求体：{}", request);

        // 限流
        long startTime = System.nanoTime();

        if (!ttsCircuitBreaker.tryAcquirePermission()) {
            resilienceTuner.recordRejection("tts");
            return Mono.error(new RuntimeException("TTS服务繁忙，请稍后重试"));
        }

        if (!ttsBulkhead.tryAcquirePermission()) {
            resilienceTuner.recordRejection("tts");
            return Mono.error(new RuntimeException("TTS服务繁忙，请稍后重试"));
        }

        if (!ttsRateLimiter.acquirePermission()) {
            resilienceTuner.recordRejection("tts");
            return Mono.error(new RuntimeException("TTS服务繁忙，请稍后重试"));
        }

        resilienceTuner.recordCall("tts");

        return ttsWebClient
                .post()
                .uri("/voice/tts")
                .header("Authorization", "Bearer " + ttsProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("TTS API返回错误状态：{}，响应体：{}", response.statusCode(), body);
                                    return Mono.error(new RuntimeException("TTS API错误：" + response.statusCode() + " - " + body));
                                })
                )
                .bodyToMono(TtsResponse.class)
                .doOnNext(response -> log.debug("收到TTS响应，reqid：{}，数据长度：{}", 
                        response.getReqid(), response.getAudio() != null ? response.getAudio().length() : 0))
                .flatMap(
                        response -> {
                            try {
                                // 将base64音频数据解码为字节数组
                                byte[] audioBytes = java.util.Base64.getDecoder().decode(response.getAudio());
                                // 生成文件名
                                String fileName = "tts_" + System.currentTimeMillis() + ".mp3";
                                log.debug("开始上传音频文件：{}，大小：{} bytes", fileName, audioBytes.length);
                                return ossService.uploadFile(audioBytes, fileName);
                            } catch (Exception e) {
                                log.error("解码音频数据失败", e);
                                return Mono.error(new RuntimeException("音频数据解码失败：" + e.getMessage()));
                            }
                        }
                )
                .retryWhen(
                        Retry.backoff(5, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(10))
                                .filter(throwable -> {
                                    // 不重试参数错误和认证错误
                                    if (throwable instanceof IllegalArgumentException) {
                                        log.warn("参数错误，不进行重试：{}", throwable.getMessage());
                                        return false;
                                    }
                                    // 记录重试信息
                                    log.warn("TTS请求失败，准备重试：{}", throwable.getMessage());
                                    return true;
                                })
                                .doBeforeRetry(retrySignal -> 
                                        log.info("TTS重试第{}次", 
                                                retrySignal.totalRetries() + 1))
                )
                //.doOnSuccess(audioUrl -> log.info("文本转语音成功，URL：{}", audioUrl))
                .doOnSuccess(
                        audioUrl -> {
                            ttsCircuitBreaker.onSuccess(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                            log.info("文本转语音成功，URL：{}", audioUrl);
                        }
                )
                .doOnError(error -> {
                    ttsCircuitBreaker.onError(System.nanoTime() - startTime, TimeUnit.NANOSECONDS, error);
                    log.error("文本转语音最终失败，文本：{}，错误详情：", text, error);
                    // 记录更多上下文信息
                    log.error("TTS配置 - API URL: {}, 超时: {}", ttsProperties.getApiUrl(), ttsProperties.getTimeout());
                })
                .doFinally(signalType -> ttsBulkhead.releasePermission())
                .onErrorReturn("TTS服务暂时不可用，请稍后重试");
    }

    public Mono<String> textToSpeech(String text) {
        return textToSpeech(text, "qiniu_zh_female_wwxkjx", 1.0f);
    }

    public Mono<String> generateTaskResponse(String taskTitle) {
        return textToSpeech("正在为您" + taskTitle);
    }
}

