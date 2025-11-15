package com.eugenezhu.voxforge.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: User
 * @author: zhuyuchen
 * @description: 用户实体类
 * @date: 2025/10/21 上午10:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("users") // 关联数据库表
@Schema(description = "用户实体")
public class User {
    @Id
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Column("username")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @Column("email")
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Column("password_hash")
    @Schema(description = "密码哈希值", example = "pbkdf2:sha256:50000$...")
    private String passwordHash;

    @Column("created_at")
    @Schema(description = "创建时间", example = "2023-10-23 10:23:23")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2023-10-23 10:23:23")
    private LocalDateTime updatedAt;

    @Column("is_active")
    @Schema(description = "是否激活", example = "true")
    private Boolean isActive;

    @Schema(description = "角色", example = "admin")
    private String role;
}

