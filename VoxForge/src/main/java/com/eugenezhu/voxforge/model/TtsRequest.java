package com.eugenezhu.voxforge.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: TtsRequest
 * @author: zhuyuchen
 * @description: 文本转语音请求对象
 * @date: 2025/10/21 下午7:58
 */
@Data
public class TtsRequest {
    private AudioConfig audio;
    private RequestConfig request;

    public TtsRequest(String text, String voiceType, Float speedRatio) {
        this.audio = new AudioConfig(
                voiceType != null ? voiceType : "qiniu_zh_female_wwxkjx",
                "mp3",
                speedRatio != null ? speedRatio : 1.0f
        );
        this.request = new RequestConfig(text);
    }

    @Data
    @AllArgsConstructor
    public static class AudioConfig {
        @JsonProperty("voice_type")
        private String voiceType;
        @JsonProperty("encoding")
        private String encoding;
        @JsonProperty("speed_ratio")
        private Float speedRatio;
    }

    @Data
    @AllArgsConstructor
    public static class RequestConfig {
        private String text;
    }
}

