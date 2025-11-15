package com.eugenezhu.voxforge.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.model
 * @className: ResponseDto
 * @author: zhuyuchen
 * @description: 服务端响应数据传输对象
 * @date: 2025/10/21 下午3:03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "服务端响应数据传输对象")
public class ResponseDto {
    @Schema(description = "会话ID", example = "12345")
    private String sessionId;
    @Schema(description = "响应文本", example = "正在为您打开记事本")
    private String text; // 响应文本
    @Schema(description = "响应音频URL", example = "https://example.com/audio.mp3")
    private String audioUrl; // 响应音频URL
    @Schema(description = "当前任务需要执行的命令", example = "start notepad.exe")
    private String cmd; // 当前任务需要执行的命令
    @Schema(description = "任务链版本号", example = "0")
    private Integer chainVersion; // 任务链版本号(因为用户可能新增任务或修改任务)
    @Schema(description = "任务清单列表")
    private List<TaskItem> taskList; // 任务清单列表
    @Schema(description = "当前任务状态")
    private TaskFeedback taskFeedback; // 当前任务状态
    @Schema(description = "错误信息")
    private String errorMsg; // 错误信息

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "任务反馈信息")
    public static class TaskFeedback {
        @Schema(description = "任务ID", example = "1")
        private Long taskId; // 任务ID
        @Schema(description = "任务状态", example = "PENDING", allowableValues = {"PENDING", "READY", "PROMPTED", "WAITING_CLIENT", "SUCCESS", "FAILED", "SKIPPED", "RETRYING"})
        private String status; // 任务状态, PENDING, READY, PROMPTED, WAITING_CLIENT, SUCCESS, FAILED, SKIPPED, RETRYING
        @Schema(description = "当前步骤", example = "1")
        private Integer currentStep; // 当前步骤
        @Schema(description = "总步骤数", example = "5")
        private Integer totalSteps; // 总步骤数
        @Schema(description = "任务状态描述", example = "任务已准备就绪")
        private String message; // 任务状态描述
    }
}

