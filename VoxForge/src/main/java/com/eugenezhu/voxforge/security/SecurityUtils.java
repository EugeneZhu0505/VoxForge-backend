package com.eugenezhu.voxforge.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.security
 * @className: SecurityUtils
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 下午7:40
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取当前用户名
     * @return
     */
    public static Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext() // 获取上下文
                .map(context -> context.getAuthentication().getName()) //  提取用户名
                .doOnNext(username -> log.info("获取当前用户名: {}", username))
                .onErrorReturn("anonymous"); // 获取失败则返回匿名用户名
    }

    /**
     * 获取当前用户ID
     * @return
     */
    public static Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext() // 获取上下文
                .map(context -> (Long) context.getAuthentication().getDetails()) //  提取用户 ID, 转为 Long
                .doOnNext(userId -> log.info("获取当前用户ID: {}", userId))
                .switchIfEmpty(Mono.error(new RuntimeException("获取当前用户ID失败")));
    }

    /**
     * 获取当前用户角色
     * @return
     */
    public static Mono<String> getCurrentRole() {
        return ReactiveSecurityContextHolder.getContext() // 获取上下文
                .map(
                        context -> {
                            return context.getAuthentication().getAuthorities().stream()
                                    .findFirst()
                                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                                    .orElse("USER");
                        }
                )
                .doOnNext(role -> log.info("获取当前用户角色: {}", role))
                .onErrorReturn("USER");
    }

    public static Mono<Boolean> isAuthenticated() {
        return ReactiveSecurityContextHolder.getContext()
                .map(
                        context -> {
                            return context.getAuthentication().isAuthenticated();
                        }
                )
                .doOnNext(authenticated -> log.info("当前用户是否已认证: {}", authenticated))
                .defaultIfEmpty(false);
    }
}

