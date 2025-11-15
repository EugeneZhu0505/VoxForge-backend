package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.model.AuthDto;
import com.eugenezhu.voxforge.model.User;
import com.eugenezhu.voxforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.service
 * @className: AuthService
 * @author: zhuyuchen
 * @description: 处理用户注册和登录
 * @date: 2025/10/22 下午7:02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户注册
     * @param request
     * @return
     */
    public Mono<AuthDto.AuthResponse> register(AuthDto.RegisterRequest request) {
        log.info("用户注册: username={}, email={}", request.getUsername(), request.getEmail());

        return userRepository.existsByUsername(request.getUsername()) // 检查用户名是否已存在
                .flatMap(
                        exists -> {
                            if (exists) {
                                return Mono.error(new RuntimeException("用户名已存在"));
                            }
                            // 检查邮箱是否已存在
                            return userRepository.existsByEmail(request.getEmail());
                        }
                ) // 返回的是邮箱是否已存在
                .flatMap(
                        exists -> {
                            if (exists) {
                                return Mono.error(new RuntimeException("邮箱已存在"));
                            }
                            return createUser(request); // 创建并返回用户对象
                        }
                )
                .flatMap(
                        user -> {
                            // 为用户生成JWT
                            String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
                            AuthDto.UserInfo userInfo = mapToUserInfo(user);
                            return Mono.just(new AuthDto.AuthResponse(token, "Bearer", userInfo));
                        }
                )
                .doOnSuccess(response -> log.info("用户注册成功: username={}, email={}", request.getUsername(), request.getEmail()))
                .doOnError(error -> log.error("用户注册失败: {}", error.getMessage()));
    }


    /**
     * 用户登录
     * @param request
     * @return
     */
    public Mono<AuthDto.AuthResponse> login(AuthDto.LoginRequest request) {
        log.info("用户登录: username={}", request.getUsername());

        return findByUsernameOrEmail(request.getUsername()) // 查找到用户
                .flatMap(
                        user -> {
                            // 验证密码
                            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                                return Mono.error(new RuntimeException("密码错误"));
                            }
                            // 检测用户是否激活
                            if (!user.getIsActive()) {
                                return Mono.error(new RuntimeException("用户已被禁用"));
                            }
                            // 直接返回用户对象
                            return Mono.just(user);
                        }
                )
                .flatMap(
                        user -> {
                            // 为用户生成JWT
                            String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole());
                            AuthDto.UserInfo userInfo = mapToUserInfo(user);
                            return Mono.just(new AuthDto.AuthResponse(token, "Bearer", userInfo));
                        }
                )
                .doOnSuccess(response -> log.info("用户登录成功: username={}", request.getUsername()))
                .doOnError(error -> log.error("用户登录失败: {}", error.getMessage()));
    }

    private Mono<User> findByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return userRepository.findByEmail(usernameOrEmail);
        } else {
            return userRepository.findByUsername(usernameOrEmail);
        }
    }


    private Mono<User> createUser(AuthDto.RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private AuthDto.UserInfo mapToUserInfo(User user) {
        AuthDto.UserInfo userInfo = new AuthDto.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(user.getRole());
        userInfo.setIsActive(user.getIsActive());
        return userInfo;
    }
}

