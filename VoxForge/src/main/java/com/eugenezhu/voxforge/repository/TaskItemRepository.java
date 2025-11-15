package com.eugenezhu.voxforge.repository;

import com.eugenezhu.voxforge.model.TaskItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.repository
 * @className: TaskItemRepository
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 上午12:40
 */
@Repository
public interface TaskItemRepository extends R2dbcRepository<TaskItem, Long> {

    /**
     * 根据任务链ID查询任务项
     * @param taskChainId
     * @return
     */
    Flux<TaskItem> findByTaskChainId(Long taskChainId);

    /**
     * 根据任务链ID和步骤顺序查询任务项
     * @param taskChainId
     * @param stepOrder
     * @return
     */
    Mono<TaskItem> findByTaskChainIdAndStepOrder(Long taskChainId, Integer stepOrder);

    /**
     * 根据任务链ID查询任务项, 按步骤顺序排序
     * @param taskChainId
     * @return
     */
    @Query("SELECT * FROM task_items WHERE task_chain_id = :taskChainId ORDER BY order_index ASC")
    Flux<TaskItem> findByTaskChainIdOrderByStepOrder(@Param("taskChainId") Long taskChainId);

    /**
     * 根据用户ID查询任务项
     * @param userId
     * @return
     */
    Flux<TaskItem> findByUserId(Long userId);

    /**
     * 根据会话ID查询任务项
     * @param sessionId
     * @return
     */
    Flux<TaskItem> findBySessionId(Long sessionId);

    /**
     * 根据任务链ID和状态查询任务项
     * @param taskChainId
     * @param status
     * @return
     */
    Flux<TaskItem> findByTaskChainIdAndStatus(Long taskChainId, String status);



    /**
     * 根据任务链ID查询下一个待处理任务项
     * @param taskChainId
     * @return
     */
    @Query("SELECT * FROM task_items WHERE task_chain_id = :taskChainId AND status = 'PENDING' ORDER BY order_index ASC LIMIT 1")
    Mono<TaskItem> findNextPendingTask(@Param("taskChainId") Long taskChainId);

    /**
     * 更新任务项状态
     * @param id
     * @param status
     * @return
     */
    @Query("UPDATE task_items SET status = :status, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新任务项执行结果
     * @param id
     * @param result
     * @return
     */
    @Query("UPDATE task_items SET result = :result, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateResult(@Param("id") Long id, @Param("result") String result);

    /**
     * 更新任务项状态和执行结果
     * @param id
     * @param status
     * @param result
     * @return
     */
    @Query("UPDATE task_items SET status = :status, result = :result, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateStatusAndResult(@Param("id") Long id, @Param("status") String status, @Param("result") String result);

    /**
     * 增加任务项重试次数
     * @param id
     * @return
     */
    @Query("UPDATE task_items SET retry_count = retry_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> incrementRetries(@Param("id") Long id);

    /**
     * 根据任务链ID查询任务项数量
     * @param taskChainId
     * @return
     */
    @Query("SELECT COUNT(*) FROM task_items WHERE task_chain_id = :taskChainId")
    Mono<Long> countByTaskChainId(@Param("taskChainId") Long taskChainId);

    /**
     * 根据任务链ID和状态查询任务项数量
     * @param taskChainId
     * @param status
     * @return
     */
    @Query("SELECT COUNT(*) FROM task_items WHERE task_chain_id = :taskChainId AND status = :status")
    Mono<Long> countByTaskChainIdAndStatus(@Param("taskChainId") Long taskChainId, @Param("status") String status);

    /**
     * 根据任务链ID删除任务项
     * @param taskChainId
     * @return
     */
    @Query("DELETE FROM task_items WHERE task_chain_id = :taskChainId")
    Mono<Integer> deleteByTaskChainId(@Param("taskChainId") Long taskChainId);
}
