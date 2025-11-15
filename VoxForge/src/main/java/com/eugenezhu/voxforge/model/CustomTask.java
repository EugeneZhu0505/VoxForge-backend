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
 * @className: CustomTask
 * @author: zhuyuchen
 * @description: 用户自定义任务
 * @date: 2025/10/21 下午1:43
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("custom_tasks")
public class CustomTask {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("name")
    private String name; // 任务名称
    @Column("description")
    private String description;
    @Column("cmd_json")
    private String cmdJson; // CMD命令列表（JSON格式）
    @Column("parameters")
    private String parameters;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
}

