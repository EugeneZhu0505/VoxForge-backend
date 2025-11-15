package com.eugenezhu.voxforge.config;

import com.eugenezhu.voxforge.security.JwtAuthenticationWebFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.config
 * @className: SecurityConfig
 * @author: zhuyuchen
 * @description: 用户认证配置类, 使用 spring security webflux, 定义 jwt-based 认证过滤器, 支持 reactive 安全
 * @date: 2025/10/21 下午4:47
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable) // 禁用 csrf 防护, 因为使用 jwt 认证, 不依赖 cookie
                // 配置 cors 策略, 跨域
                .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
                .authorizeExchange(
                        exchanges -> exchanges
                                // 允许 认证 请求不进行认证
                                .pathMatchers("/auth/**").permitAll()
                                // 允许 健康检查 请求不进行认证
                                .pathMatchers("/health").permitAll()
                                // 允许 swagger 请求不进行认证
                                .pathMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                                // 允许 webjars 静态资源 (Swagger UI 依赖)
                                .pathMatchers("/webjars/**").permitAll()
                                // 其他所有请求都需要认证
                                .anyExchange().authenticated()
                )
                // 添加 jwt 认证过滤器
                .addFilterBefore(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public JwtProperties jwtProperties() {
        return new JwtProperties(secret, expiration);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(List.of("*")); // 允许所有域名跨域
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 允许的请求方法
        corsConfiguration.setAllowedHeaders(List.of("*")); // 允许所有头信息
        corsConfiguration.setAllowCredentials(true); // 允许携带 cookie
        // 注册 cors 配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration); // 对所有路径应用 cors 配置
        return source;
    }

    @Data
    @AllArgsConstructor
    public static class JwtProperties {
        private final String secret;
        private final Long expiration;
    }
}

