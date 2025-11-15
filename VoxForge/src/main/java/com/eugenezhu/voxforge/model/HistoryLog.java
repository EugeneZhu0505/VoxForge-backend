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
 * @className: HistoryLog
 * @author: zhuyuchen
 * @description: 历史日志实体，包含会话 id、任务链、时间戳、结果（成功/失败）
 * @date: 2025/10/21 下午1:39
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("history_logs")
public class HistoryLog {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("session_id")
    private Long sessionId;
    @Column("task_chain_id")
    private Long taskChainId;
    @Column("task_id")
    private Long taskId;
    @Column("action_type")
    private String actionType; // 操作类型
    @Column("status")
    private String status; // 状态: SUCCESS, FAILED
    @Column("message")
    private String message; // 日志消息
    @Column("error_message")
    private String errorMessage; // 错误消息
    @Column("created_at")
    private LocalDateTime createdAt;
}

