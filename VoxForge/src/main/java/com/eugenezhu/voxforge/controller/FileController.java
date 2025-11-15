package com.eugenezhu.voxforge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.controller
 * @className: FileController
 * @author: zhuyuchen
 * @description: 文件服务控制器，提供文件访问接口（重定向到七牛云）
 * @date: 2025/10/22 下午4:30
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "File API", description = "文件服务接口")
public class FileController {

    @Value("${oss.qiniu.domain}")
    private String qiniuDomain;

    @GetMapping("/{filename:.+}")
    @Operation(summary = "获取文件", description = "重定向到七牛云文件URL")
    public Mono<ResponseEntity<Void>> getFile(
            @Parameter(description = "文件名") @PathVariable String filename) {

        return Mono.fromCallable(() -> {
            try {
                // 构建七牛云文件URL
                String fileUrl = "https://" + qiniuDomain + "/" + filename;
                log.info("重定向到七牛云文件: {}", fileUrl);
                
                // 返回重定向响应
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(fileUrl))
                        .build();
            } catch (Exception e) {
                log.error("构建文件URL失败: {}, 错误: {}", filename, e.getMessage());
                return ResponseEntity.notFound().build();
            }
        });
    }
}

