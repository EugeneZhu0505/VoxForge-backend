package com.eugenezhu.voxforge.security;

import com.eugenezhu.voxforge.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.security
 * @className: JwtAuthenticationWebFilter
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 下午8:35
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 跳过不需要认证的路径
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 从请求头中提取JWT令牌
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = jwtService.extractTokenFromHeader(authHeader);

        if (token == null) {
            log.warn("请求缺少JWT令牌: {}", path);
            return handleUnauthorized(exchange);
        }

        try {
            // 验证令牌并提取用户信息
            String username = jwtService.extractUsername(token);
            Long userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            if (username != null && jwtService.validateToken(token, username)) {
                // 创建认证对象
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // 将用户ID添加到认证详情中
                authentication.setDetails(userId);

                // 设置安全上下文并继续处理请求
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            } else {
                log.warn("JWT令牌验证失败: {}", path);
                return handleUnauthorized(exchange);
            }
        } catch (Exception e) {
            log.error("JWT令牌处理异常: {}, error: {}", path, e.getMessage());
            return handleUnauthorized(exchange);
        }
    }

    /**
     * 判断是否为公开路径
     * @param path
     * @return
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/") ||
                path.startsWith("/files/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/api/swagger-ui/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api/v3/api-docs") ||
                path.startsWith("/webjars/") ||
                path.startsWith("/api/webjars/") ||
                path.equals("/swagger-ui.html") ||
                path.equals("/api/swagger-ui.html") ||
                path.endsWith("/health");
    }

    /**
     * 处理未授权的请求
     * @param exchange
     * @return
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String body = "{\"error\":\"未授权访问\",\"message\":\"请提供有效的JWT令牌\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }
}

