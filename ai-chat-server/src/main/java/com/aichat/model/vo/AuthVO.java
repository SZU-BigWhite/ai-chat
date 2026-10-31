package com.aichat.model.vo;

/** 登录或注册成功后的会话。 */
public record AuthVO(String token, UserVO user) {
    @Override
    public String toString() {
        return "AuthVO[token=REDACTED]";
    }
}
