package com.eugenezhu.voxforge.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: AuthDto
 * @author: zhuyuchen
 * @description: 用户注册, 登录, 认证等数据传输对象
 * @date: 2025/10/22 下午5:36
 */
public class AuthDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor

    public static class RegisterRequest {

        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 20, message = "用户名长度必须在2到20之间")
        @Schema(description = "用户名", example = "username")
        private String username;

        @NotBlank(message = "邮箱不能为空")
        @Size(min = 6, max = 50, message = "邮箱长度必须在6到50之间")
        @Schema(description = "邮箱", example = "username@example.com")
        private String email;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度必须在6到20之间")
        @Schema(description = "密码", example = "password")
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "用户登录请求数据")
    public static class LoginRequest {

        @NotBlank(message = "用户名不能为空")
        @Schema(description = "用户名或邮箱", example = "username")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Schema(description = "密码", example = "password")
        private String password;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "用户认证响应数据")
    public static class AuthResponse {

        @Schema(description = "JWT访问令牌")
        private String accessToken;

        @Schema(description = "JWT令牌类型", example = "Bearer")
        private String tokenType;

        @Schema(description = "用户信息")
        private UserInfo userInfo;

        public AuthResponse(String accessToken, UserInfo userInfo) {
            this.accessToken = accessToken;
            this.userInfo = userInfo;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "用户信息")
    public static class UserInfo {

        @Schema(description = "用户ID", example = "1")
        private Long id;

        @Schema(description = "用户名", example = "username")
        private String username;

        @Schema(description = "邮箱", example = "username@example.com")
        private String email;

        @Schema(description = "用户角色", example = "ROLE_USER")
        private String role;

        @Schema(description = "用户是否激活", example = "true")
        private Boolean isActive;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "通用响应")
    public static class MessageResponse {

        @Schema(description = "响应消息", example = "操作成功")
        private String message;

    }
}

