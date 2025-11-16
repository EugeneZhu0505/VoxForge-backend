package com.eugenezhu.voxforge.service;

import com.eugenezhu.voxforge.model.*;
import com.eugenezhu.voxforge.repository.SessionRepository;
import com.eugenezhu.voxforge.repository.TaskChainRepository;
import com.eugenezhu.voxforge.repository.TaskItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eugenezhu.voxforge.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.service
 * @className: TaskChainService
 * @author: zhuyuchen
 * @description: 核心任务链服务, 解析 llm 输出为任务链, 维护任务链状态
 * @date: 2025/10/21 下午9:07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskChainService {
    private final LlmService llmService;
    private final TtsService ttsService;
    private final SessionRepository sessionRepository;
    private final TaskChainRepository taskChainRepository;
    private final TaskItemRepository taskItemRepository;

    /**
     * 调用 llm 解析用户输入, 获得输出, 生成任务链
     * @param text
     * @param sessionId
     * @param clientEnv 客户端环境和系统信息（JSON对象）
     * @param userId
     * @return
     */
    public Mono<ResponseDto> parseUserInput(String text, Long sessionId, java.util.Map<String, Object> clientEnv, Long userId) {
        log.info("开始解析用户输入创建任务链，用户ID: {}, 会话ID: {}", userId, sessionId);

        return llmService.parseUserInput(text, clientEnv)  // 调用 llm 解析用户输入
                .flatMap(llmResponse -> {
                    // 解析 llm 输出为任务链
                    TaskChain taskChain = createTaskChain(userId, sessionId);

                    return taskChainRepository.save(taskChain)
                            .flatMap(
                                    savedTaskChain -> {
                                        // 更新会话的任务链ID
                                        return sessionRepository.findById(sessionId)
                                                .flatMap(session -> {
                                                    session.setChainId(savedTaskChain.getId());
                                                    session.setUpdatedAt(LocalDateTime.now());
                                                    return sessionRepository.save(session);
                                                })
                                                .then(Mono.just(savedTaskChain));
                                    }
                            )
                            .flatMap(
                                    savedTaskChain -> {
                                        // 创建任务清单
                                        List<TaskItem> tasks = createTaskItems(llmResponse.getTasks(), savedTaskChain.getId(), userId, sessionId);
                                        return Flux.fromIterable(tasks)
                                                .flatMap(taskItemRepository::save)
                                                .collectList()
                                                .flatMap(
                                                        savedTasks -> {
                                                            // 生成第一个任务的语音提示
                                                            if (!savedTasks.isEmpty()) {
                                                                TaskItem firstTask = savedTasks.get(0);
                                                                firstTask.setStatus("READY");
                                                                return taskItemRepository.save(firstTask)
                                                                        .flatMap(
                                                                                updatedTask ->
                                                                                        ttsService.generateTaskResponse(updatedTask.getTitle())
                                                                                                .map(audioUrl -> buildInitialResponse(llmResponse, savedTaskChain, savedTasks, audioUrl))
                                                                        );
                                                            } else {
                                                                // 没有任务返回空响应
                                                                return Mono.just(buildEmptyResponse(llmResponse.getReply()));
                                                            }
                                                        }
                                                );
                                    }
                            );
                })
                .doOnSuccess(response -> {
                    int taskCount = response.getTaskList() != null ? response.getTaskList().size() : 0;
                    log.info("成功创建任务链，用户ID: {}, 会话ID: {}, 共有 {} 个任务", userId, sessionId, taskCount);
                })
                .doOnError(error -> {
                    log.error("创建任务链失败，用户ID: {}, 会话ID: {}, 错误信息: {}", userId, sessionId, error.getMessage());
                });
    }

    // 动态添加任务到任务链

    /**
     * 接受客户端反馈, 执行下一个任务
     * @param session
     * @param taskFeedback
     * @return
     */
    public Mono<ResponseDto> executeTask(Session session, RequestDto.TaskFeedback taskFeedback) {
        log.info("开始执行任务，用户ID: {}, 会话ID: {}, 任务ID: {}", session.getUserId(), session.getId(), 
                taskFeedback != null ? taskFeedback.getTaskId() : "null");
        
        return applyFeedback(session, taskFeedback)
                .flatMap(
                        updatedSession -> sessionRepository.save(updatedSession)
                                .flatMap(
                                        savedSession -> getNextTask(savedSession)
                                                .flatMap(
                                                        task -> {
                                                            if (task != null) {
                                                                log.info("找到下一个任务: {}, 状态: {}", task.getId(), task.getStatus());
                                                                // 更新任务状态为 READY
                                                                task.setStatus("READY");
                                                                task.setUpdatedAt(LocalDateTime.now());
                                                                return taskItemRepository.save(task)
                                                                        .flatMap(
                                                                                savedTask -> {
                                                                                    // 更新会话的当前任务ID
                                                                                    savedSession.setCurrentTaskId(savedTask.getId());
                                                                                    savedSession.setUpdatedAt(LocalDateTime.now());
                                                                                    return sessionRepository.save(savedSession)
                                                                                            .then(ttsService.generateTaskResponse(savedTask.getTitle())
                                                                                                    .onErrorReturn("") // 如果TTS失败，返回空字符串而不是null
                                                                                            )
                                                                                            .map(audioUrl -> buildTaskResponse(savedTask, audioUrl, savedSession))
                                                                                            .switchIfEmpty(Mono.just(buildTaskResponse(savedTask, "", savedSession))); // 确保不返回null
                                                                                }
                                                                        );
                                                            } else {
                                                                log.info("所有任务执行完毕，返回完成响应");
                                                                // 所有任务执行完毕，返回完成响应
                                                                return taskChainRepository.findBySessionId(savedSession.getId())
                                                                        .flatMap(
                                                                                taskChain -> {
                                                                                    taskChain.setStatus("COMPLETED");
                                                                                    taskChain.setUpdatedAt(LocalDateTime.now());
                                                                                    taskChain.setCompletedAt(LocalDateTime.now());
                                                                                    return taskChainRepository.save(taskChain);
                                                                                }
                                                                        )
                                                                        .then(Mono.fromCallable(() -> {
                                                                            // 更新会话状态
                                                                            savedSession.setStatus("COMPLETED");
                                                                            savedSession.setCurrentTaskId(null);
                                                                            savedSession.setUpdatedAt(LocalDateTime.now());
                                                                            return savedSession;
                                                                        }))
                                                                        .flatMap(sessionRepository::save)
                                                                        .then(ttsService.textToSpeech("所有任务已完成")
                                                                                .onErrorReturn("") // 如果TTS失败，返回空字符串而不是null
                                                                        )
                                                                        .map(audioUrl -> buildCompletionResponse(audioUrl, savedSession))
                                                                        .switchIfEmpty(Mono.just(buildCompletionResponse("", savedSession))); // 确保不返回null
                                                            }
                                                        }
                                                )
                                )
                )
                .doOnSuccess(response -> {
                    if (response != null) {
                        log.info("任务执行完成，返回响应: sessionId={}", response.getSessionId());
                    } else {
                        log.warn("任务执行完成，但响应为null");
                    }
                })
                .doOnError(error -> log.error("任务执行失败: sessionId={}, error={}", session.getId(), error.getMessage()))
                .onErrorResume(error -> {
                    log.error("executeTask发生异常，返回错误响应", error);
                    ResponseDto errorResponse = new ResponseDto();
                    errorResponse.setSessionId(session.getId().toString());
                    errorResponse.setText("任务执行出现错误");
                    errorResponse.setErrorMsg(error.getMessage());
                    return Mono.just(errorResponse);
                })
                .switchIfEmpty(
                    // 如果整个流程返回空，创建一个默认响应并调用TTS
                    ttsService.textToSpeech("任务处理完成")
                        .onErrorReturn("") // 如果TTS失败，返回空字符串
                        .map(audioUrl -> {
                            log.warn("executeTask返回空响应，创建默认响应");
                            ResponseDto defaultResponse = new ResponseDto();
                            defaultResponse.setSessionId(session.getId().toString());
                            defaultResponse.setText("任务处理完成");
                            defaultResponse.setAudioUrl(audioUrl);
                            return defaultResponse;
                        })
                );
    }

    private ResponseDto buildCompletionResponse(String audioUrl, Session session) {
        ResponseDto response = new ResponseDto();
        response.setSessionId(session.getId().toString());
        response.setText("所有任务已完成");
        response.setAudioUrl(audioUrl);
        response.setTaskList(List.of());
        // 设置任务状态
        ResponseDto.TaskFeedback taskFeedback = new ResponseDto.TaskFeedback();
        taskFeedback.setStatus("COMPLETED");
        taskFeedback.setMessage("所有任务已完成");
        response.setTaskFeedback(taskFeedback);
        return response;
    }

    private ResponseDto buildTaskResponse(TaskItem task, String audioUrl, Session session) {
        ResponseDto response = new ResponseDto();
        response.setSessionId(session.getId().toString());
        response.setText("正在为您" + task.getTitle());
        response.setAudioUrl(audioUrl);
        response.setCmd(task.getCmd());
        response.setTaskList(List.of(task));
        // 设置任务状态
        ResponseDto.TaskFeedback taskFeedback = new ResponseDto.TaskFeedback();
        taskFeedback.setTaskId(task.getId());
        taskFeedback.setStatus("READY");
        taskFeedback.setCurrentStep(task.getStepOrder());
        // 获取总步骤数
        taskChainRepository.findBySessionId(session.getId())
                .flatMap(taskChain -> taskItemRepository.countByTaskChainId(taskChain.getId()))
                .subscribe(totalSteps -> taskFeedback.setTotalSteps(totalSteps.intValue()));
        taskFeedback.setMessage("任务已准备就绪");
        response.setTaskFeedback(taskFeedback);
        return response;
    }

    /**
     * 获取下一个任务
     * @param session
     * @return
     */
    private Mono<TaskItem> getNextTask(Session session) {
        return taskChainRepository.findBySessionId(session.getId()) // 获取任务链
                .flatMap(
                        taskChain -> {
                            // 查找下一个待执行的任务
                            return taskItemRepository.findNextPendingTask(taskChain.getId())
                                    .switchIfEmpty(
                                            // 如果没有找到待执行任务，查找失败但可重试的任务
                                            taskItemRepository.findByTaskChainIdAndStatus(taskChain.getId(), "FAILED")
                                                    .filter(task -> task.getRetries() < task.getMaxRetries())
                                                    .next()
                                    );
                        }
                );
    }

    /**
     * 应用客户端反馈到任务链
     * @param session
     * @param taskFeedback
     * @return
     */
    private Mono<Session> applyFeedback(Session session, RequestDto.TaskFeedback taskFeedback) {
        if (taskFeedback == null) {
            return Mono.just(session); // 无反馈, 直接返回会话
        }

        log.info("应用任务反馈，用户ID: {}, 会话ID: {}, 任务ID: {}, 反馈: {}", session.getUserId(), session.getId(), taskFeedback.getTaskId(), taskFeedback);

        return taskItemRepository.findById(taskFeedback.getTaskId())
                .flatMap(
                        task -> {
                            switch (taskFeedback.getStatus().toUpperCase()) {
                                case "SUCCESS":
                                    // 任务成功, 更新任务状态为成功
                                    log.info("任务成功，任务ID: {}, 回复: {}", task.getId(), taskFeedback.getFeedback());
                                    task.setStatus("SUCCESS");
                                    task.setResult(taskFeedback.getFeedback());
                                    task.setUpdatedAt(LocalDateTime.now());
                                    break;
                                case "FAILED":
                                    log.info("任务失败，任务ID: {}, 错误信息: {}", task.getId(), taskFeedback.getFeedback());
                                    task.setStatus("FAILED");
                                    task.setResult(taskFeedback.getFeedback());
                                    task.setRetries(task.getRetries() + 1);
                                    task.setUpdatedAt(LocalDateTime.now());
                                    break;
                                case "RETRY":
                                    // 重试任务
                                    log.info("任务 {} 需要重试", taskFeedback.getTaskId());
                                    task.setStatus("PENDING");
                                    task.setRetries(task.getRetries() + 1);
                                    task.setUpdatedAt(LocalDateTime.now());
                                    break;
                                default:
                                    log.warn("未知的任务状态: {}", taskFeedback.getStatus());
                            }
                            return taskItemRepository.save(task);
                        }
                )
                .then(
                        Mono.fromCallable(
                                () -> {
                                    // 更新会话状态
                                    session.setCurrentTaskId(taskFeedback.getTaskId());
                                    session.setUpdatedAt(LocalDateTime.now());
                                    return session;
                                }
                        )
                );
    }

    private ResponseDto buildEmptyResponse(String reply) {
        ResponseDto response = new ResponseDto();
        response.setText(reply != null ? reply : "处理完成");
        response.setSessionId(""); // 设置默认sessionId
        response.setTaskList(List.of()); // 设置空的任务列表，避免空指针异常
        return response;
    }

    private ResponseDto buildInitialResponse(LlmResponse llmResponse, TaskChain taskChain, List<TaskItem> tasks, String audioUrl) {
        ResponseDto response = new ResponseDto();
        response.setSessionId(taskChain.getSessionId().toString());
        response.setText("正在为您" + tasks.get(0).getTitle());
        response.setCmd(tasks.get(0).getCmd());
        response.setChainVersion(taskChain.getVersion());
        response.setTaskList(tasks);
        response.setAudioUrl(audioUrl);
        // 设置任务状态
        ResponseDto.TaskFeedback taskFeedback = new ResponseDto.TaskFeedback();
        taskFeedback.setTaskId(tasks.get(0).getId());
        taskFeedback.setStatus("READY");
        taskFeedback.setCurrentStep(0);
        taskFeedback.setTotalSteps(tasks.size());
        taskFeedback.setMessage("任务已准备就绪");
        response.setTaskFeedback(taskFeedback);

        return response;
    }

    private List<TaskItem> createTaskItems(List<LlmResponse.TaskDefinition> taskDefinitions, Long taskChainId, Long userId, Long sessionId) {
        AtomicInteger stepOrder = new AtomicInteger(0); // 任务链中的步骤顺序
        List<TaskItem> tasks = new ArrayList<>();

        for (LlmResponse.TaskDefinition taskDef : taskDefinitions) {
            TaskItem taskItem = new TaskItem();
            taskItem.setTitle(taskDef.getTitle());
            taskItem.setCmd(taskDef.getCmd());
            taskItem.setStatus("PENDING");
            taskItem.setStepOrder(stepOrder.getAndIncrement());
            taskItem.setRetries(0);
            taskItem.setMaxRetries(taskDef.getMaxRetries() != null ? taskDef.getMaxRetries() : 3);
            taskItem.setTaskChainId(taskChainId);
            taskItem.setUserId(userId);
            taskItem.setSessionId(sessionId);
            taskItem.setCreatedAt(LocalDateTime.now());
            taskItem.setUpdatedAt(LocalDateTime.now());
            tasks.add(taskItem);
        }
        return tasks;
    }

    /**
     * 新建任务链
     * @param userId
     * @param sessionId
     * @return
     */
    private TaskChain createTaskChain(Long userId, Long sessionId) {
        TaskChain taskChain = new TaskChain();
        taskChain.setUserId(userId);
        taskChain.setSessionId(sessionId);
        taskChain.setCurrentIndex(0); // 初始任务索引为 0
        taskChain.setVersion(0); // 初始版本为 0
        taskChain.setStatus("PENDING"); // 初始状态为 PENDING
        taskChain.setCreatedAt(LocalDateTime.now());
        taskChain.setUpdatedAt(LocalDateTime.now());
        return taskChain;
    }
}

