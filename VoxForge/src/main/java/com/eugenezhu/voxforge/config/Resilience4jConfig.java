package com.eugenezhu.voxforge.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.config
 * @className: Resilience4jConfig
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/11/16 下午1:48
 */
@Configuration
public class Resilience4jConfig {

    /**
     * 注册默认的 CircuitBreaker, 断路器模式
     * @return
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults(); // 注册默认的 CircuitBreaker
    }

    /**
     * 注册默认的 Bulkhead, 舱壁隔离
     * @return
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        return BulkheadRegistry.ofDefaults(); // 注册默认的 Bulkhead
    }

    /**
     * 注册默认的 Retry
     * @return
     */
    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults(); // 注册默认的 Retry
    }

    /**
     * 注册 ASR 服务的 CircuitBreaker
     * 相当于为asr服务注册一个断路器, 当失败率超过50%时, 打开断路器, 拒绝请求, 10秒后尝试半开状态, 允许5次调用, 5次调用成功后, 断路器关闭, 否则继续打开断路器
     * @return
     */
    @Bean("asrCircuitBreaker")
    public CircuitBreaker asrCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig cfg = CircuitBreakerConfig.custom() // 自定义 CircuitBreaker 配置
                .failureRateThreshold(50f) // 失败率阈值为 50%, 超过 50% 失败时打开断路器
                .waitDurationInOpenState(Duration.ofSeconds(10)) // 断路器打开后 10 秒内拒绝请求
                .permittedNumberOfCallsInHalfOpenState(5) // 半开状态下允许 5 次调用
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED) // 基于时间的滑动窗口
                .slidingWindowSize(30) // 滑动窗口大小为 30 次调用
                .build();
        return registry.circuitBreaker("asrApi", cfg);
    }

    @Bean("ttsCircuitBreaker")
    public CircuitBreaker ttsCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig cfg = CircuitBreakerConfig.custom()
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(30)
                .build();
        return registry.circuitBreaker("ttsApi", cfg);
    }

    @Bean("llmCircuitBreaker")
    public CircuitBreaker llmCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig cfg = CircuitBreakerConfig.custom()
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(30)
                .build();
        return registry.circuitBreaker("llmApi", cfg);
    }

    /**
     * 注册 ASR 服务的 Bulkhead, 舱壁隔离
     * 相当于为asr服务注册一个舱壁, 最大并发调用数为 10, 超过 10 个并发调用时, 其他调用会被阻塞, 直到有空闲槽位
     * @param registry
     * @return
     */
    @Bean("asrBulkHead")
    public Bulkhead asrBulkHead(BulkheadRegistry registry) {
        BulkheadConfig cfg = BulkheadConfig.custom()
                .maxConcurrentCalls(10) // 最大并发调用数为 10
                .maxWaitDuration(Duration.ofMillis(100)) // 最大等待时间为 100 毫秒
                .build();
        return registry.bulkhead("asrApi", cfg);
    }

    @Bean("ttsBulkHead")
    public Bulkhead ttsBulkHead(BulkheadRegistry registry) {
        BulkheadConfig cfg = BulkheadConfig.custom()
                .maxConcurrentCalls(10) // 最大并发调用数为 10
                .maxWaitDuration(Duration.ofMillis(100)) // 最大等待时间为 100 毫秒
                .build();
        return registry.bulkhead("ttsApi", cfg);
    }

    @Bean("llmBulkHead")
    public Bulkhead llmBulkHead(BulkheadRegistry registry) {
        BulkheadConfig cfg = BulkheadConfig.custom()
                .maxConcurrentCalls(50) // 最大并发调用数为 50
                .maxWaitDuration(Duration.ofMillis(200)) // 最大等待时间为 200 毫秒
                .build();
        return registry.bulkhead("llmApi", cfg);
    }

    /**
     * 注册 ASR 服务的 RateLimiter, 限流
     * 相当于为asr服务注册一个限流器, 每个时间周期允许 20 个调用, 超过 20 个调用时, 其他调用会被阻塞, 直到有空闲槽位
     * @param registry
     * @return
     */
    @Bean("asrRateLimiter")
    public RateLimiter asrRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig cfg = RateLimiterConfig.custom()
                .limitForPeriod(20) // 每个时间周期允许 20 个调用
                .limitRefreshPeriod(Duration.ofSeconds(1)) // 时间周期为 1 秒
                .timeoutDuration(Duration.ofMillis(200)) // 超时时间为 200 毫秒, 超过 200 毫秒未获取到槽位, 则抛出异常
                .build();
        return registry.rateLimiter("asrApi", cfg);
    }

    @Bean("ttsRateLimiter")
    public RateLimiter ttsRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig cfg = RateLimiterConfig.custom()
                .limitForPeriod(20) // 每个时间周期允许 20 个调用
                .limitRefreshPeriod(Duration.ofSeconds(1)) // 时间周期为 1 秒
                .timeoutDuration(Duration.ofMillis(200)) // 超时时间为 200 毫秒, 超过 200 毫秒未获取到槽位, 则抛出异常
                .build();
        return registry.rateLimiter("ttsApi", cfg);
    }

    @Bean("llmRateLimiter")
    public RateLimiter llmRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig cfg = RateLimiterConfig.custom()
                .limitForPeriod(50) // 每个时间周期允许 50 个调用
                .limitRefreshPeriod(Duration.ofSeconds(1)) // 时间周期为 1 秒
                .timeoutDuration(Duration.ofMillis(200)) // 超时时间为 200 毫秒, 超过 200 毫秒未获取到槽位, 则抛出异常
                .build();
        return registry.rateLimiter("llmApi", cfg);
    }

    /**
     * 注册 ASR 服务的 Retry, 重试
     * 相当于为asr服务注册一个重试器, 最多重试 3 次, 每次重试等待 100 毫秒, 超过 3 次重试仍失败, 则抛出异常 RuntimeException
     * @param registry
     * @return
     */
    @Bean("asrRetry")
    public Retry asrRetry(RetryRegistry registry) {
        RetryConfig cfg = RetryConfig.custom()
                .maxAttempts(3) // 最大重试次数为 3
                .waitDuration(Duration.ofMillis(100)) // 重试等待时间为 100 毫秒
                .retryExceptions(RuntimeException.class)
                .build();
        return registry.retry("asrApi", cfg);
    }

    @Bean("ttsRetry")
    public Retry ttsRetry(RetryRegistry registry) {
        RetryConfig cfg = RetryConfig.custom()
                .maxAttempts(3) // 最大重试次数为 3
                .waitDuration(Duration.ofMillis(100)) // 重试等待时间为 100 毫秒
                .retryExceptions(RuntimeException.class)
                .build();
        return registry.retry("ttsApi", cfg);
    }

    @Bean("llmRetry")
    public Retry llmRetry(RetryRegistry registry) {
        RetryConfig cfg = RetryConfig.custom()
                .maxAttempts(3) // 最大重试次数为 3
                .waitDuration(Duration.ofMillis(100)) // 重试等待时间为 100 毫秒
                .retryExceptions(RuntimeException.class)
                .build();
        return registry.retry("llmApi", cfg);
    }
}

