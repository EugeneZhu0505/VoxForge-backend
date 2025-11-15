package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.model.RequestDto;
import com.eugenezhu.voxforge.model.ResponseDto;
import com.eugenezhu.voxforge.model.Session;
import com.eugenezhu.voxforge.security.SecurityUtils;
import com.eugenezhu.voxforge.repository.SessionRepository;
import com.eugenezhu.voxforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.service
 * @className: VoxForgeService
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 上午3:55
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoxForgeService {

    private final AsrService asrService;
    private final TtsService ttsService;
    private final LlmService llmService;
    private final OssService ossService;
    private final JwtService jwtService;
    private final TaskChainService taskChainService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public Mono<ResponseDto> parseUserInput(RequestDto request) {
        log.info("处理用户输入: sessionId={}", request.getSessionId());

        return getSession(request) // 获取会话
                .flatMap(
                        session -> {
                            return parseUserAudio(request)
                                    .flatMap(
                                            text -> {
                                                return taskChainService.parseUserInput(text, session.getId(), request.getClientEnv(), session.getUserId());
                                            }
                                    );
                        }
                )
                .doOnSuccess(response -> log.info("处理用户输入完成: sessionId={}", request.getSessionId()))
                .doOnError(error -> log.error("处理用户输入失败: sessionId={}, error={}", request.getSessionId(), error.getMessage()));
    }

    public Mono<ResponseDto> handleTaskFeedback(RequestDto request) {
        log.info("收到任务反馈请求: sessionId={}, taskId={}, status={}", 
                request.getSessionId(), 
                request.getTaskFeedback() != null ? request.getTaskFeedback().getTaskId() : "null",
                request.getTaskFeedback() != null ? request.getTaskFeedback().getStatus() : "null");
        
        return getSession(request)
                .doOnNext(session -> log.info("找到会话: sessionId={}, status={}, currentTaskId={}", 
                        session.getId(), session.getStatus(), session.getCurrentTaskId()))
                .flatMap(session -> taskChainService.executeTask(session, request.getTaskFeedback()))
                .doOnSuccess(response -> log.info("处理任务反馈完成: sessionId={}, responseText={}", 
                        response.getSessionId(), response.getText()))
                .doOnError(error -> log.error("处理任务反馈失败: sessionId={}, error={}", 
                        request.getSessionId(), error.getMessage(), error));
    }

    public Mono<String> uploadFile(Mono<FilePart> filePart) {
        log.info("开始上传文件");
        return filePart.flatMap(ossService::uploadFile)
                .doOnSuccess(url -> log.info("文件上传成功: url={}", url))
                .doOnError(error -> log.error("文件上传失败: {}", error.getMessage()));
    }

    private Mono<String> parseUserAudio(RequestDto request) {
        // 用户输入的是文本，则直接返回
        if (request.getText() != null && !request.getText().isEmpty()) {
            return Mono.just(request.getText());
        } else if (request.getAudioUrl() != null && !request.getAudioUrl().isEmpty()){
            log.info("处理用户语音输入: auduiUrl={}", request.getAudioUrl());
            return asrService.speechToTextStream(request.getAudioUrl(), request.getAudioFormat());
        } else {
            return Mono.error(new IllegalArgumentException("请提供文本或语音输入"));
        }
    }

    private Mono<Session> getSession(RequestDto request) {
        // 如果没有sessionId，则创建一个新会话
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            return createNewSession(request.getUserToken());
        }
        // 验证对话
        return sessionRepository.findById(Long.parseLong(request.getSessionId()))
                .switchIfEmpty(Mono.error(new RuntimeException("会话不存在: " + request.getSessionId())))
                .flatMap(
                        session -> {
                            return SecurityUtils.getCurrentUserId()
                                    .flatMap(
                                            userId -> {
                                                if (!session.getUserId().equals(userId)) {
                                                    return Mono.error(new RuntimeException("用户未授权: " + userId));
                                                }
                                                // 更新会话的clientEnv信息
                                                if (request.getClientEnv() != null) {
                                                    session.setClientEnv(request.getClientEnv());
                                                    session.setUpdatedAt(LocalDateTime.now());
                                                    return sessionRepository.save(session);
                                                }
                                                return Mono.just(session);
                                            }
                                    );
                        }
                );
    }

    private Mono<Session> createNewSession(String userToken) {
        log.info("创建新会话: userToken={}",  userToken);

        return SecurityUtils.getCurrentUserId()
                .flatMap(
                        userId -> {
                            Session session = new Session();
                            session.setUserId(userId);
                            session.setSessionToken(UUID.randomUUID().toString());
                            session.setStatus("ACTIVE");
                            session.setClientEnv(new HashMap<>()); // 初始化为空的JSON对象
                            session.setCreatedAt(LocalDateTime.now());
                            session.setUpdatedAt(LocalDateTime.now());
                            return sessionRepository.save(session) // 保存会话
                                    .doOnSuccess(
                                            savedSession -> log.info("新会话创建成功: sessionId={}", savedSession.getId())
                                    );
                        }
                );
    }
}

