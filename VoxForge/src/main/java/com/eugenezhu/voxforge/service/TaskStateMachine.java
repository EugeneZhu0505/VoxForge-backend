package com.eugenezhu.voxforge.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.service
 * @className: TaskStateMachine
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/11/17 下午3:27
 */
@Component
public class TaskStateMachine {

    private final Map<String, Map<String, String>> table = Map.of(
            "PENDING", Map.of("START", "IN_PROGRESS"),
            "IN_PROGRESS", Map.of("COMPLETE", "COMPLETED", "FAIL", "FAILED"),
            "FAILED", Map.of("RETRY", "IN_PROGRESS"),
            "COMPLETED", Map.of()
    );

    public String next(String current, String event) {
        Map<String, String> nextState = table.getOrDefault(current, Map.of());
        return nextState.getOrDefault(event, current);
    }
}

