package com.eugenezhu.voxforge.repository;

import com.eugenezhu.voxforge.model.TaskChain;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.repository
 * @className: TaskChainRepository
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 上午12:36
 */
@Repository
public interface TaskChainRepository extends R2dbcRepository<TaskChain, Long> {

    /**
     * 根据用户ID查询任务链
     * @param userId
     * @return
     */
    Flux<TaskChain> findByUserId(Long userId);

    /**
     * 根据会话ID查询任务链
     * @param sessionId
     * @return
     */
    Mono<TaskChain> findBySessionId(Long sessionId);

    /**
     * 根据用户ID和状态查询任务链
     * @param userId
     * @param status
     * @return
     */
    Flux<TaskChain> findByUserIdAndStatus(Long userId, String status);

    /**
     * 根据用户ID查询最新的任务链
     * @param userId
     * @return
     */
    @Query("SELECT * FROM task_chains WHERE user_id = :userId ORDER BY created_at DESC LIMIT 1")
    Mono<TaskChain> findLatestByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询活跃的任务链（状态为PENDING或IN_PROGRESS）
     * @param userId
     * @return
     */
    @Query("SELECT * FROM task_chains WHERE user_id = :userId AND status IN ('PENDING', 'IN_PROGRESS') ORDER BY created_at DESC")
    Flux<TaskChain> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 更新任务链状态
     * @param id
     * @param status
     * @return
     */
    @Query("UPDATE task_chains SET status = :status, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新任务链当前执行任务索引
     * @param id
     * @param currentIndex
     * @return
     */
    @Query("UPDATE task_chains SET current_index = :currentIndex, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateCurrentIndex(@Param("id") Long id, @Param("currentIndex") Integer currentIndex);

    /**
     * 完成任务链（将状态设置为COMPLETED）
     * @param id
     * @return
     */
    @Query("UPDATE task_chains SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> completeTaskChain(@Param("id") Long id);

    /**
     * 根据用户ID查询任务链数量
     * @param userId
     * @return
     */
    @Query("SELECT COUNT(*) FROM task_chains WHERE user_id = :userId")
    Mono<Long> countByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID删除任务链
     * @param userId
     * @return
     */
    @Query("DELETE FROM task_chains WHERE user_id = :userId")
    Mono<Integer> deleteByUserId(@Param("userId") Long userId);


}

