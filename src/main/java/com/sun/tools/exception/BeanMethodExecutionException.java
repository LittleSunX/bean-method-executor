package com.sun.tools.exception;

/**
 * 自定义异常类
 */
public class BeanMethodExecutionException extends RuntimeException {
    public BeanMethodExecutionException(String message) {
        super(message);
    }

    public BeanMethodExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}