package com.eugenezhu.voxforge.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: RequestDto
 * @author: zhuyuchen
 * @description: 用户请求数据传输对象
 * @date: 2025/10/21 下午2:58
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户请求数据传输对象")
public class RequestDto {

    @Schema(description = "会话ID(可选, 新会话为空)", example = "demo_session_001")
    private String sessionId; // 会话ID

    @Schema(description = "用户令牌", example = "eyJhbGciOiJIUzI1NiJ9.demo_token")
    private String userToken; // 用户令牌

    @Schema(description = "音频URL(可选)", example = "https://example.com/audio.mp3")
    private String audioUrl; // 音频URL, 公网链接

    @Schema(description = "音频格式(可选)", example = "mp3")
    private String audioFormat;

    @Schema(description = "文本内容(可选)", example = "播放我最喜欢的音乐")
    private String text; // 文本内容, 可选

    @Schema(description = "客户端环境和系统信息，合并为一个JSON对象", example = "{\"os\":\"Windows 11\",\"browser\":\"Chrome\",\"version\":\"1.0.0\"}")
    private Map<String, Object> clientEnv; // 客户端环境和系统信息，合并为一个JSON对象

    @Schema(description = "任务反馈(可选)")
    private TaskFeedback taskFeedback; // 任务反馈

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "任务反馈")
    public static class TaskFeedback {

        @Schema(description = "任务ID", example = "1234567890")
        private Long taskId; // 任务ID

        @Schema(description = "执行状态", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED", "RETRY"})
        private String status; // success, failed, retry

        @Schema(description = "任务状态描述", example = "记事本已成功打开")
        private String feedback; // 反馈内容

        @Schema(description = "任务输出内容", example = "C:\\Users\\username\\Documents\\notes.txt")
        private String output; // 输出内容
    }
}

