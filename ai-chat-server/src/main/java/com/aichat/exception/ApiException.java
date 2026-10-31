package com.aichat.exception;

import org.springframework.http.HttpStatus;

/** 可向客户端展示的业务异常，HTTP 状态与内部原因分离保存。 */
public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;

    public ApiException(HttpStatus status, String message) {
        this(status.value(), message);
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        this(status.value(), message, cause);
    }

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException missing() {
        return new ApiException(HttpStatus.NOT_FOUND, "内容不存在或无权访问");
    }

    public static ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "知识树已更新，请刷新后重试；原输入仍保留在页面中");
    }
}
