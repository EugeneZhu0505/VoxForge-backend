package com.eugenezhu.voxforge.repository;

import com.eugenezhu.voxforge.model.Session;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.repository
 * @className: SessionRepository
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 上午12:48
 */
@Repository
public interface SessionRepository extends R2dbcRepository<Session, Long> {

    /**
     * 根据用户ID查询会话
     * @param userId
     * @return
     */
    Flux<Session> findByUserId(Long userId);

    /**
     * 根据会话令牌查询会话
     * @param sessionToken
     * @return
     */
    Mono<Session> findBySessionToken(String sessionToken);

    /**
     * 根据会话ID查询会话
     * @param chainId
     * @return
     */
    Mono<Session> findByChainId(Long chainId);

    /**
     * 根据用户ID和状态查询会话
     * @param userId
     * @param status
     * @return
     */
    Flux<Session> findByUserIdAndStatus(Long userId, String status);

    /**
     * 根据用户ID查询最新的会话
     * @param userId
     * @return
     */
    @Query("SELECT * FROM sessions WHERE user_id = :userId ORDER BY created_at DESC LIMIT 1")
    Mono<Session> findLatestByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询活跃的会话（状态为ACTIVE或WAITING）
     * @param userId
     * @return
     */
    @Query("SELECT * FROM sessions WHERE user_id = :userId AND status IN ('ACTIVE', 'WAITING') ORDER BY updated_at DESC")
    Flux<Session> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 更新会话状态
     * @param id
     * @param status
     * @return
     */
    @Query("UPDATE sessions SET status = :status, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新会话当前任务ID
     * @param id
     * @param currentTaskId
     * @return
     */
    @Query("UPDATE sessions SET current_task_id = :currentTaskId, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateCurrentTaskId(@Param("id") Long id, @Param("currentTaskId") Long currentTaskId);

    /**
     * 更新会话最后回复
     * @param id
     * @param lastReply
     * @return
     */
    @Query("UPDATE sessions SET last_reply = :lastReply, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateLastReply(@Param("id") Long id, @Param("lastReply") String lastReply);

    /**
     * 增加会话重试次数
     * @param id
     * @return
     */
    @Query("UPDATE sessions SET retries = retries + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> incrementRetries(@Param("id") Long id);

    /**
     * 根据用户ID查询会话数量
     * @param userId
     * @return
     */
    @Query("SELECT COUNT(*) FROM sessions WHERE user_id = :userId")
    Mono<Long> countByUserId(@Param("userId") Long userId);

    /**
     * 查询超时会话（状态为ACTIVE或WAITING，且更新时间超过指定分钟数）
     * @param timeoutMinutes
     * @return
     */
    @Query("SELECT * FROM sessions WHERE status IN ('ACTIVE', 'WAITING') AND updated_at < CURRENT_TIMESTAMP - INTERVAL ':timeoutMinutes minutes'")
    Flux<Session> findTimeoutSessions(@Param("timeoutMinutes") Integer timeoutMinutes);

    /**
     * 根据用户ID删除会话
     * @param userId
     * @return
     */
    @Query("DELETE FROM sessions WHERE user_id = :userId")
    Mono<Integer> deleteByUserId(@Param("userId") Long userId);
}
