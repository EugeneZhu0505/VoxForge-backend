package com.eugenezhu.voxforge.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: AsrRequest
 * @author: zhuyuchen
 * @description: 发送给七牛云的ASR请求体
 * @date: 2025/10/21 下午7:00
 */
@Data
public class AsrRequest {
    private String model = "asr";
    private AudioInfo audio;  // 修改字段名为 audio

    public AsrRequest(String audioUrl, String audioFormat) {
        this.audio = new AudioInfo(audioFormat, audioUrl);  // 修正参数顺序：format在前，url在后
    }

    @Data
    @AllArgsConstructor
    public static class AudioInfo {
        private String format;  // format 字段在前
        private String url;     // url 字段在后，与官方文档顺序一致
    }
}

