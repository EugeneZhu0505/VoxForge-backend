package com.eugenezhu.voxforge.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.config
 * @className: AiConfig
 * @author: zhuyuchen
 * @description: llm 配置类
 * @date: 2025/10/21 下午4:05
 */
@Configuration
public class AiConfig {
    @Value("${external.llm.api-key}")
    private String apiKey;
    @Value("${external.llm.model}")
    private String model;
    @Value("${external.llm.timeout}")
    private String timeout;

    @Bean
    public LlmProperties llmProperties() {
        return new LlmProperties(apiKey, model, timeout);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LlmProperties {
        private String apiKey;
        private String model;
        private String timeout;
    }
}

