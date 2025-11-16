package com.eugenezhu.voxforge.ratelimit;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Semaphore;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.ratelimit
 * @className: SemaphoreRateLimiter
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/11/15 下午10:06
 */
public class SemaphoreRateLimiter {

    private final Semaphore semaphore;

    /**
     * 构造函数
     * @param permits 许可证数量
     */
    public SemaphoreRateLimiter(int permits) {
        this.semaphore = new Semaphore(permits, true); // 公平模式, 先到先得
    }

    public <T> Mono<T> limit(Mono<T> source) {
        return Mono.fromCallable(() -> { // 创建一个mono, 当被订阅时触发
                    semaphore.acquire();
                    return true; // 成功获取许可证
                })
                .subscribeOn(Schedulers.boundedElastic()) // 指定阻塞操作的线程池, 弹性线程池, 是reactor提供的, 用于执行阻塞操作, 如数据库查询, 文件读写等; 不会阻塞事件循环线程如schedulers.parallel()
                .flatMap(ignore -> source) // 成功获取许可证后, 执行原始操作, 即返回原始的mono
                .doFinally(sig -> semaphore.release()); // 无论成功或失败, 都释放许可证
    }
}

