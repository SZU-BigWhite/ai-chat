package com.aichat.model.vo;

import java.time.Instant;

/** 公开账号信息，不包含密码哈希。 */
public record UserVO(String id, String email, String name, String role, Boolean enabled, Instant createdAt) {
}
