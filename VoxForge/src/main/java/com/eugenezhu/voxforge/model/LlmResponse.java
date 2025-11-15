package com.eugenezhu.voxforge.model;

import lombok.Data;

import java.util.List;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: LlmResponse
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/21 下午8:26
 */
@Data
public class LlmResponse {
    private String reply;
    private List<TaskDefinition> tasks;

    @Data
    public static class TaskDefinition {
        private String title;
        private String cmd;
        private Integer maxRetries;
    }
}

