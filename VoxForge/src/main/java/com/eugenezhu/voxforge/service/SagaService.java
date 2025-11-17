package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.model.TaskItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @projectName: VoxForge
 * @package: com.eugenezhu.voxforge.service
 * @className: SagaService
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/11/17 下午3:44
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaService {
    // 存储每个 saga 实例的任务栈
    private final ConcurrentHashMap<Long, Deque<TaskItem>> stacks = new ConcurrentHashMap<>();

    public void recordTaskSuccess(Long sessionId, TaskItem taskItem) {
        stacks.computeIfAbsent(sessionId, k -> new ArrayDeque<>()).push(taskItem);
    }

    /**
     * 回滚指定会话的所有已成功执行的任务，返回需要执行的补偿命令列表
     * @param sessionId
     * @return
     */
    public List<String> rollback(Long sessionId) {
        Deque<TaskItem> stack = stacks.getOrDefault(sessionId, new ArrayDeque<>());
        List<String> cmds = new ArrayList<>();
        while (!stack.isEmpty()) {
            TaskItem taskItem = stack.pop();
            cmds.add(compensate(taskItem));
        }
        return cmds;
    }

    /**
     * 为给定的任务项生成对应的补偿命令
     * @param t
     * @return
     */
    private String compensate(TaskItem t) {
        String undo = t.getUndoCmd();
        if (undo != null && !undo.isBlank()) return undo;
        String c = t.getCmd();
        if (c == null) return "echo rollback";
        if (c.startsWith("start ")) {
            String name = c.substring(6).trim();
            return "taskkill /IM " + name + " /F";
        }
        return "echo rollback: " + t.getTitle();
    }
}

