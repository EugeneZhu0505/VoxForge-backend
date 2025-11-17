package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.config.OssConfig;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.service
 * @className: OssService
 * @author: zhuyuchen
 * @description: 七牛云oss服务, 用于上传文件到七牛云oss
 * @date: 2025/10/22 上午12:24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {
    
    private final OssConfig ossConfig;
    private final Auth qiniuAuth;
    private final UploadManager uploadManager;
    
    @Value("${oss.file-prefix}")
    private String filePrefix;

    public Mono<String> uploadFile(FilePart filePart) {
        return filePart.content() // 获取FilePart中的数据
                .collectList() // 转换为List<DataBuffer>
                .flatMap(
                        dataBuffers -> {
                            try {
                                // 合并所有data buffer
                                int totalSize = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
                                byte[] bytes = new byte[totalSize];
                                int offset = 0;
                                for (DataBuffer dataBuffer : dataBuffers) {
                                    int length = dataBuffer.readableByteCount();
                                    dataBuffer.read(bytes, offset, length);
                                    offset += length;
                                }
                                return uploadFile(bytes, filePart.filename());
                            } catch (Exception e) {
                                log.error("处理文件数据失败: {}", e.getMessage(), e);
                                return Mono.error(new RuntimeException("处理文件数据失败", e));
                            }
                        }
                );
    }

    /**
     * 上传文件到七牛云oss
     * @param file
     * @return
     */
    public Mono<String> uploadFile(MultipartFile file) {
        return Mono.fromCallable(
                () -> {
                    if (file == null || file.isEmpty()) {
                        throw new IllegalArgumentException("文件不能为空");
                    }
                    try {
                        // 生成唯一文件名
                        String fileName = generateFileName(file.getOriginalFilename());
                        // 获取上传凭证
                        String upToken = qiniuAuth.uploadToken(ossConfig.getBucket());
                        // 上传文件到七牛云
                        Response response = uploadManager.put(file.getBytes(), fileName, upToken);
                        if (response.isOK()) {
                            // 生成文件访问URL
                            String fileUrl = "http://" + ossConfig.getDomain() + "/" + fileName;
                            log.info("文件上传成功，文件URL: {}", fileUrl);
                            return fileUrl;
                        } else {
                            log.error("七牛云上传失败: {}", response.bodyString());
                            throw new RuntimeException("七牛云上传失败: " + response.bodyString());
                        }
                    } catch (QiniuException e) {
                        log.error("七牛云上传异常: {}", e.getMessage(), e);
                        throw new RuntimeException("七牛云上传异常", e);
                    } catch (Exception e) {
                        log.error("文件上传失败: {}", e.getMessage(), e);
                        throw new RuntimeException("文件上传失败", e);
                    }
                }
        ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<String> uploadFile(byte[] fileData, String originalFileName) {
        return Mono.fromCallable(
                () -> {
                    if (fileData == null || fileData.length == 0) {
                        throw new IllegalArgumentException("文件数据不能为空");
                    }
                    try {
                        // 生成唯一文件名
                        String fileName = generateFileName(originalFileName);
                        // 获取上传凭证
                        String upToken = qiniuAuth.uploadToken(ossConfig.getBucket());
                        // 上传文件到七牛云
                        Response response = uploadManager.put(fileData, fileName, upToken);
                        if (response.isOK()) {
                            // 生成文件访问URL
                            String fileUrl = "http://" + ossConfig.getDomain() + "/" + fileName;
                            log.info("文件上传成功，文件URL: {}", fileUrl);
                            return fileUrl;
                        } else {
                            log.error("七牛云上传失败: {}", response.bodyString());
                            throw new RuntimeException("七牛云上传失败: " + response.bodyString());
                        }
                    } catch (QiniuException e) {
                        log.error("七牛云上传异常: {}", e.getMessage(), e);
                        throw new RuntimeException("七牛云上传异常", e);
                    } catch (Exception e) {
                        log.error("文件上传失败: {}", e.getMessage(), e);
                        throw new RuntimeException("文件上传失败", e);
                    }
                }
        ).subscribeOn(Schedulers.boundedElastic());
    }

    private String generateFileName(String originalFilename) {
        // 获取文件扩展名
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 生成时间戳和UUID
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 组合文件名：前缀 + 时间戳 + UUID + 扩展名
        return filePrefix + "_" + timestamp + "_" + uuid + extension;
    }
}

