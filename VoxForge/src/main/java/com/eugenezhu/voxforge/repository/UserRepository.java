package com.eugenezhu.voxforge.repository;

import com.eugenezhu.voxforge.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.repository
 * @className: UserRepository
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 上午1:58
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {
    /**
     * 根据用户名查询用户
     * @param username
     * @return
     */
    Mono<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email
     * @return
     */
    Mono<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     * @param username
     * @return
     */
    @Query("SELECT COUNT(*) > 0 FROM users WHERE username = :username")
    Mono<Boolean> existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     * @param email
     * @return
     */
    @Query("SELECT COUNT(*) > 0 FROM users WHERE email = :email")
    Mono<Boolean> existsByEmail(@Param("email") String email);

    /**
     * 更新密码
     * @param id
     * @param passwordHash
     * @return
     */
    @Query("UPDATE users SET password_hash = :passwordHash, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /**
     * 激活用户
     * @param id
     * @return
     */
    @Query("UPDATE users SET is_active = true, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> activateUser(@Param("id") Long id);

    /**
     * 禁用用户
     * @param id
     * @return
     */
    @Query("UPDATE users SET is_active = false, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> deactivateUser(@Param("id") Long id);
}
