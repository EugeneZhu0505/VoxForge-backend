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
 * @className: TaskChain
 * @author: zhuyuchen
 * @description: 任务链实体, 包含任务清单列表, 用户id, 会话id等
 * @date: 2025/10/21 下午1:20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("task_chains")
public class TaskChain {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("session_id")
    private Long sessionId;
    @Column("current_index")
    private Integer currentIndex; // 当前任务索引
    @Column("version")
    private Integer version; // 任务链版本（用于并发控制）
    @Column("status")
    private String status; // 状态: PENDING, IN_PROGRESS, COMPLETED, FAILED
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("completed_at")
    private LocalDateTime completedAt;

}

