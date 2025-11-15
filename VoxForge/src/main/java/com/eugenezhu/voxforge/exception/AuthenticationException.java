package com.eugenezhu.voxforge.exception;

/**
 * @projectName: VoxForge
 * @package: com.hzau.voxforge.exception
 * @className: AuthenticationException
 * @author: zhuyuchen
 * @description: TODO
 * @date: 2025/10/22 下午8:18
 */
public class AuthenticationException extends RuntimeException{

    /**
     * 创建一个 AuthenticationException
     * @param message
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * 创建一个带 cause 的 AuthenticationException
     * @param message
     * @param cause
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

