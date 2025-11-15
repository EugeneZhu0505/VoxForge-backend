package com.eugenezhu.voxforge.model;

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
 * @className: Session
 * @author: zhuyuchen
 * @description: 会话实体, 包含用户id, 会话id, 任务链id等
 * @date: 2025/10/21 下午1:29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("sessions")
public class Session {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("chain_id")
    private Long chainId; // 当前任务链ID
    @Column("current_task_id")
    private Long currentTaskId; // 当前任务ID
    @Column("client_env")
    private java.util.Map<String, Object> clientEnv; // 客户端环境信息（包含系统信息的JSON对象）
    @Column("last_reply")
    private String lastReply; // 最后回复
    @Column("retries")
    private Integer retries; // 重试次数
    @Column("status")
    private String status; // 状态: ACTIVE, INACTIVE, EXPIRED
    @Column("session_token")
    private String sessionToken;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}

