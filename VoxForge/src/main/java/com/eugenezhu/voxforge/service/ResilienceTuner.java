package com.eugenezhu.voxforge.service;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.service
 * @className: ResilienceTuner
 * @author: zhuyuchen
 * @description:
 * @date: 2025/11/16 下午9:29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilienceTuner {

    @Qualifier("asrRateLimiter")
    private final RateLimiter asrRateLimiter;

    @Qualifier("ttsRateLimiter")
    private final RateLimiter ttsRateLimiter;

    @Qualifier("llmRateLimiter")
    private final RateLimiter llmRateLimiter;

    @Qualifier("asrBulkhead")
    private final Bulkhead asrBulkhead;

    @Qualifier("ttsBulkhead")
    private final Bulkhead ttsBulkhead;

    @Qualifier("llmBulkhead")
    private final Bulkhead llmBulkhead;

    private final ConcurrentHashMap<String, AtomicLong> rejects = new ConcurrentHashMap<>(); // 记录拒绝请求的次数
    private final ConcurrentHashMap<String, AtomicLong> calls = new ConcurrentHashMap<>(); // 记录成功调用的次数

    public void recordRejection(String key) {
        rejects.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet(); // 拒绝请求次数加一
    }

    public void recordCall(String key) {
        calls.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet(); // 成功调用次数加一
    }

    /**
     * 调整限流策略
     */
    @Scheduled(fixedDelay = 60000) // 每 60 秒调整一次
    public void adjust() {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean(); // 获取内存管理 bean
        long used = mem.getHeapMemoryUsage().getUsed(); // 获取堆内存已使用量
        long max = mem.getHeapMemoryUsage().getMax(); // 获取堆内存最大量
        // 计算使用率
        double memRatio = (double) used / max; // 计算堆内存使用率

        ThreadMXBean tm = ManagementFactory.getThreadMXBean(); // 获取线程管理 bean
        int threads = tm.getThreadCount(); // 获取当前线程数

        // 根据内存使用率和线程数调整限流
        tune("asr", asrRateLimiter, asrBulkhead, memRatio, threads);
        tune("tts", ttsRateLimiter, ttsBulkhead, memRatio, threads);
        tune("llm", llmRateLimiter, llmBulkhead, memRatio, threads);

        reset();
    }

    /**
     * 调整限流策略
     * @param key 限流键
     * @param rateLimiter 限流器
     * @param bulkhead 舱壁
     * @param memRatio 内存使用率
     * @param threads 线程数
     */
    private void tune(String key, RateLimiter rateLimiter, Bulkhead bulkhead, double memRatio, int threads) {
        long rejectNum = rejects.getOrDefault(key, new AtomicLong(0)).get(); // 获取拒绝请求次数
        long callNum = calls.getOrDefault(key, new AtomicLong(0)).get(); // 获取成功调用次数
        double rejectRatio = (double) rejectNum / (callNum + rejectNum); // 计算拒绝请求率

        boolean high = memRatio > 0.7 || threads > 100 || rejectRatio > 0.3; // 内存使用率超过 70% 或线程数超过 100 个或拒绝请求率超过 30%
        boolean low = memRatio < 0.3 && threads < 50 && rejectRatio < 0.1; // 内存使用率低于 30% 且线程数低于 50 个且拒绝请求率低于 10%

        if (high) {
            try {
                // 内存使用率或线程数或拒绝请求率超过阈值, 则降低限流
                rateLimiter.changeLimitForPeriod(Math.max(1, rateLimiter.getRateLimiterConfig().getLimitForPeriod() - 5));
                // 超时时间也相应减少, 但不能低于 200ms
                rateLimiter.changeTimeoutDuration(Duration.ofMillis(Math.min(rateLimiter.getRateLimiterConfig().getTimeoutDuration().toMillis(), 200)));
                bulkhead.changeConfig(
                        BulkheadConfig.custom()
                                .maxConcurrentCalls(Math.max(1, bulkhead.getBulkheadConfig().getMaxConcurrentCalls() - 1)) // 并发调用数减一, 但不能低于 1
                                .maxWaitDuration(Duration.ofMillis(100)) // 最大等待时间 100ms
                                .build()
                );
            } catch (Exception e) {
                log.error("Failed to tune rate limiter and bulkhead for key: {}", key, e);
            }
        } else if (low) {
            try {
                // 内存使用率或线程数或拒绝请求率低于阈值, 则提高限流
                rateLimiter.changeLimitForPeriod(Math.min(rateLimiter.getRateLimiterConfig().getLimitForPeriod() + 5, 100)); // 并发调用数加一, 但不能超过 100
                // 超时时间也相应增加, 但不能超过 500ms
                rateLimiter.changeTimeoutDuration(Duration.ofMillis(rateLimiter.getRateLimiterConfig().getTimeoutDuration().toMillis() + 50));
                bulkhead.changeConfig(
                        BulkheadConfig.custom()
                                .maxConcurrentCalls(Math.min(bulkhead.getBulkheadConfig().getMaxConcurrentCalls() + 2, 100)) // 并发调用数加二, 但不能超过 100
                                .maxWaitDuration(Duration.ofMillis(200)) // 最大等待时间 200ms
                                .build()
                );
            } catch (Exception e) {
                log.error("Failed to tune rate limiter and bulkhead for key: {}", key, e);
            }
        }
    }

    private void reset() {
        rejects.clear();
        calls.clear();
    }

}

