package com.eugenezhu.voxforge.model;

import lombok.Data;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: TtsResponse
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/21 下午8:02
 */
@Data
public class TtsResponse {
    private String reqid;
    private String operation;
    private Integer sequence;
    private String data; // base64编码的音频数据
    private AdditionInfo addition;

    @Data
    public static class AdditionInfo {
        private String duration; // 音频时长，单位毫秒
    }

    // 为了兼容性，添加getAudio方法
    public String getAudio() {
        return this.data;
    }
}

