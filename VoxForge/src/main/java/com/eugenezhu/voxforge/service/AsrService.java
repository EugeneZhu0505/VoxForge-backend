package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.config.AsrTtsConfig;
import com.eugenezhu.voxforge.model.AsrRequest;
import com.eugenezhu.voxforge.model.AsrResponse;
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
 * @className: AsrService
 * @author: zhuyuchen
 * @description: asr语音转文本服务, 使用webclient调用qiniu asr api, 返回mono<string>
 * @date: 2025/10/21 下午5:55
 */
@Slf4j
@Service
@RequiredArgsConstructor // 只对final字段生成构造函数
public class AsrService {
    @Qualifier("asrWebClient") // 注入asrWebClient
    private final WebClient asrWebClient;
    private final AsrTtsConfig.AsrProperties asrProperties;

    @Qualifier("asrCircuitBreaker")
    private final CircuitBreaker asrCircuitBreaker;
    //@Qualifier("asrRetry")
    //private final Retry asrRetry;
    @Qualifier("asrBulkhead")
    private final Bulkhead asrBulkhead;
    @Qualifier("asrRateLimiter")
    private final RateLimiter asrRateLimiter;

    private final ResilienceTuner resilienceTuner;

    /**
     * 调用qiniu asr api, 将语音流转换为文本流
     * @param audioUrl 语音流url
     * @param audioFormat 语音流格式, 如wav, mp3等
     * @return 文本流
     */
    public Mono<String> speechToTextStream(String audioUrl, String audioFormat) {
        log.info("开始语音转文本, audioUrl: {}, audioFormat: {}", audioUrl, audioFormat);

        // 参数验证
        if (audioUrl == null || audioUrl.trim().isEmpty()) {
            log.error("音频URL为空");
            return Mono.error(new IllegalArgumentException("音频URL不能为空"));
        }

        AsrRequest request = new AsrRequest(audioUrl, audioFormat); // 创建asr请求对象
        
        // 添加调试日志
        log.debug("发送ASR请求体：{}", request);

        // 新增限流
        long startTime = System.nanoTime();

        if (!asrCircuitBreaker.tryAcquirePermission()) {
            resilienceTuner.recordRejection("asr"); // 记录asr拒绝请求
            return Mono.error(new RuntimeException("ASR服务拒绝请求"));
        }

        if (!asrBulkhead.tryAcquirePermission()) {
            resilienceTuner.recordRejection("asr"); // 记录asr拒绝请求
            return Mono.error(new RuntimeException("ASR服务拒绝请求"));
        }

        if (!asrRateLimiter.acquirePermission()) {
            resilienceTuner.recordRejection("asr"); // 记录asr拒绝请求
            return Mono.error(new RuntimeException("ASR服务拒绝请求"));
        }

        // 成功调用, 记录调用次数
        resilienceTuner.recordCall("asr");

        // webclient调用api
        return asrWebClient
                .post() // post方法
                .uri("/voice/asr") // 调用asr api
                .header("Authorization", "Bearer " + asrProperties.getApiKey()) // 授权头, 包含api key
                .contentType(MediaType.APPLICATION_JSON) // 设置请求体为json格式
                .body(BodyInserters.fromValue(request)) // 设置请求体为request对象
                .retrieve() // 发送请求, 并返回响应体
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("ASR API返回错误状态：{}，响应体：{}", response.statusCode(), body);
                                    return Mono.error(new RuntimeException("ASR API错误：" + response.statusCode() + " - " + body));
                                })
                )
                .bodyToMono(AsrResponse.class) // 响应体转换格式为mono
                .doOnNext(response -> log.debug("收到ASR响应：{}", response))
                .map(response -> {
                    log.info("ASR API 响应: {}", response);
                    if (response != null && response.getData() != null && 
                        response.getData().getResult() != null) {
                        return response.getData().getResult().getText();
                    } else {
                        log.warn("ASR API 响应格式异常或为空");
                        return null;
                    }
                }) // 提取文本字段
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2)) // 重试3次, 每次间隔2秒
                                .maxBackoff(Duration.ofSeconds(10)) // 最大重试间隔10秒
                                .jitter(0.5) // 添加抖动以避免雷群效应
                                .filter(throwable -> {
                                    // 不重试的异常类型
                                    if (throwable instanceof IllegalArgumentException) {
                                        log.error("参数错误，不进行重试：{}", throwable.getMessage());
                                        return false;
                                    }
                                    // 对于连接重置错误，进行重试
                                    if (throwable.getCause() instanceof java.net.SocketException) {
                                        log.warn("检测到Socket异常，将进行重试：{}", throwable.getMessage());
                                        return true;
                                    }
                                    // 记录重试信息
                                    log.warn("ASR请求失败，准备重试：{}", throwable.getMessage());
                                    return true;
                                })
                                .doBeforeRetry(retrySignal -> 
                                        log.info("ASR重试第{}次，错误类型：{}", 
                                                retrySignal.totalRetries() + 1,
                                                retrySignal.failure().getClass().getSimpleName()))
                )
                //.doOnSuccess(text -> log.info("语音转文本成功, text: {}", text)) // 成功日志
                .doOnSuccess(
                        text -> {
                            asrCircuitBreaker.onSuccess(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                            log.info("语音转文本成功, text: {}", text);
                        }
                )
                .doOnError(error -> {
                    asrCircuitBreaker.onError(System.nanoTime() - startTime, TimeUnit.NANOSECONDS, error);
                    log.error("语音转文本最终失败，音频URL：{}，错误详情：", audioUrl, error);
                    // 记录更多上下文信息
                    log.error("ASR配置 - API URL: {}, 超时: {}", asrProperties.getApiUrl(), asrProperties.getTimeout());
                })
                .doFinally(
                        sig -> asrBulkhead.releasePermission() // 释放bulkhead权限
                )
                .onErrorReturn("语音转文本失败"); // 失败时返回默认值
    }
}

