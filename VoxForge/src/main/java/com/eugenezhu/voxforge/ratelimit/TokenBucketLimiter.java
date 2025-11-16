package com.eugenezhu.voxforge.ratelimit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.ratelimit
 * @className: TokenBucketLimiter
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/11/16 下午1:27
 */
public class TokenBucketLimiter {

    private final long capacity; // 令牌桶的容量
    private final long refillTokens; // 补充令牌的数量
    private final Duration refillInterval; // 补充令牌的时间间隔
    private final AtomicLong tokens; // 当前令牌桶中的令牌数量

    /**
     * 令牌桶限流器
     * @param capacity 令牌桶的容量
     * @param refillTokens 补充令牌的数量
     * @param refillInterval 补充令牌的时间间隔
     */
    public TokenBucketLimiter(long capacity, long refillTokens, Duration refillInterval) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillInterval = refillInterval;
        this.tokens = new AtomicLong(capacity); // 初始化时令牌桶满
        Flux.interval(refillInterval) // 创建定时发送的 Long 序列，每个元素间隔为 refillInterval, 就是 i
                .subscribeOn(Schedulers.parallel()) // 并行调度器，用于处理令牌 refillInterval 时间间隔的令牌 refillTokens 个令牌
                .subscribe( // 启动定时任务
                        i -> {
                            long currentTokens = tokens.get();
                            long newTokens = Math.min(currentTokens + refillTokens, capacity);
                            tokens.compareAndSet(currentTokens, newTokens);
                        }
                );
    }

    /**
     * 消耗令牌
     * @param cost 消耗的令牌数量
     * @param source 源 Mono
     * @return 源 Mono
     * @param <T> 源 Mono 元素类型
     */
    public <T> Mono<T> consume(long cost, Mono<T> source) {
        return Mono.defer(
                () -> {
                    long currentTokens = tokens.get();
                    if (currentTokens >= cost && tokens.compareAndSet(currentTokens, currentTokens - cost)) {
                        return source; // 令牌足够, cas获取成功, 返回源 Mono
                    } else {
                        return Mono.delay(refillInterval)
                                .flatMap(i -> consume(cost, source)); // 相当于轮询, 直到获取到足够的令牌
                    }
                }
        );
    }
}

