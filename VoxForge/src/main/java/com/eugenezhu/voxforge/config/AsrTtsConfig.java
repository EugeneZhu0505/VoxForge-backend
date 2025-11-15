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
 * @className: AsrTtsConfig
 * @author: zhuyuchen
 * @description: 语音识别与合成配置类
 * @date: 2025/10/21 下午4:28
 */
@Configuration
public class AsrTtsConfig {
    @Value("${external.asr.api-url}")
    private String asrApiUrl;
    @Value("${external.tts.api-url}")
    private String ttsApiUrl;
    @Value("${external.asr.api-key}")
    private String asrApiKey;
    @Value("${external.tts.api-key}")
    private String ttsApiKey;
    @Value("${external.asr.timeout}")
    private String asrTimeout;
    @Value("${external.tts.timeout}")
    private String ttsTimeout;

    @Bean
    public AsrProperties asrProperties() {
        return new AsrProperties(asrApiUrl, asrApiKey, asrTimeout);
    }

    @Bean
    public TtsProperties ttsProperties() {
        return new TtsProperties(ttsApiUrl, ttsApiKey, ttsTimeout);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AsrProperties {
        private String apiUrl;
        private String apiKey;
        private String timeout;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TtsProperties {
        private String apiUrl;
        private String apiKey;
        private String timeout;
    }
}

