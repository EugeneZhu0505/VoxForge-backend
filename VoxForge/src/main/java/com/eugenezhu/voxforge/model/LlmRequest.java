package com.eugenezhu.voxforge.model;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: LlmRequest
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/21 下午8:21
 */
@Data
public class LlmRequest {
    private String model;
    private List<Message> messages;

    public LlmRequest(String prompt, String message, String model) {
        this.model = model;
        this.messages = List.of(
                new Message("system", prompt),
                new Message("user", message)
        );
    }

    @Data
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}

