package com.eugenezhu.voxforge.controller;

import com.eugenezhu.voxforge.model.RequestDto;
import com.eugenezhu.voxforge.model.ResponseDto;
import com.eugenezhu.voxforge.service.VoxForgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.controller
 * @className: VoxForgeController
 * @author: zhuyuchen
 * @description: VoxForge 主控制器, 用于接收用户输入并执行任务
 * @date: 2025/10/22 上午2:02
 */
@Slf4j
@RestController
@RequestMapping("/voxforge")
@RequiredArgsConstructor
@Tag(name = "VoxForge API", description = "语音助手核心接口")
public class VoxForgeController {

    private final VoxForgeService voxForgeService;

    @PostMapping("/input")
    @Operation(summary = "处理用户输入", description = "接收用户语音or文输入, 解析生成任务链并返回第一个任务")
    public Mono<ResponseEntity<ResponseDto>> handleUserInput(@Valid @RequestBody RequestDto request) {
        log.info("处理用户输入: sessionId={}", request.getSessionId());

        return voxForgeService.parseUserInput(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("处理用户输入完成: sessionId={}", request.getSessionId()))
                .doOnError(error -> log.error("处理用户输入失败: sessionId={}, error={}", request.getSessionId(), error.getMessage()))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传文件到七牛云oss, 返回公网url")
    public Mono<ResponseEntity<String>> uploadFile(@Parameter(description = "语音文件") @RequestPart("file") Mono<FilePart> filePart) {
        log.info("收到文件上传请求");

        return voxForgeService.uploadFile(filePart)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("文件上传成功: url={}", response))
                .doOnError(error -> log.error("文件上传失败: {}", error.getMessage()))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    @PostMapping("/feedback")
    @Operation(summary = "处理任务反馈", description = "接收客户端任务执行反馈，更新任务状态并返回下一个任务")
    public Mono<ResponseEntity<ResponseDto>> handleTaskFeedback(@Valid @RequestBody RequestDto request) {
        log.info("处理任务反馈: sessionId={}, taskId={}", request.getSessionId(),
                request.getTaskFeedback() != null ? request.getTaskFeedback().getTaskId() : "null");

        return voxForgeService.handleTaskFeedback(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("处理任务反馈完成: sessionId={}", request.getSessionId()))
                .doOnError(error -> log.error("处理任务反馈失败: sessionId={}, error={}", request.getSessionId(), error.getMessage()))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }
}

