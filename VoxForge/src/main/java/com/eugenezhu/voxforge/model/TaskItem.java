package com.eugenezhu.voxforge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: TaskItem
 * @author: zhuyuchen
 * @description: 单个任务项
 * @date: 2025/10/21 上午10:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("task_items")
public class TaskItem {
    @Id
    private Long id;
    @Column("title")
    private String title;
    @Column("description")
    private String description;
    @Column("cmd")
    private String cmd; // 命令行命令
    @Transient
    private String undoCmd; // 撤销命令
    @Column("status")
    private String status; // 状态: PENDING, READY, PROMPTED, WAITING_CLIENT, SUCCESS, FAILED, SKIPPED, RETRYING
    @Column("task_chain_id")
    private Long taskChainId; // 属于任务链的ID
    @Column("order_index")
    private Integer stepOrder; // 在任务链的步骤顺序
    @Column("retry_count")
    private Integer retries; // 重试次数
    @Column("max_retries")
    private Integer maxRetries; // 最大重试次数
    @Column("result")
    private String result; // 执行结果
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("user_id")
    private Long userId;
    @Column("session_id")
    private Long sessionId;
}

