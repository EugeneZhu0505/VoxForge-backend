package com.eugenezhu.voxforge.controller;

import com.eugenezhu.voxforge.model.AuthDto;
import com.eugenezhu.voxforge.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.controller
 * @className: AuthController
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 下午7:27
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "用户认证接口, 包括注册登录等")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册接口, 创建用户")
    public Mono<ResponseEntity<AuthDto.AuthResponse>> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        log.info("用户注册: username={}", request.getUsername());
        return authService.register(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("用户注册成功: username={}", request.getUsername()));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口, 获取用户令牌")
    public Mono<ResponseEntity<AuthDto.AuthResponse>> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        log.info("用户登录: username={}", request.getUsername());
        return authService.login(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("用户登录成功: username={}", request.getUsername()));
    }
}

