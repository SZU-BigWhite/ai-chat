package com.aichat.model.vo;

import java.time.Instant;

/** AI 调用统计，不暴露提示词或回答。 */
public record AiUsageVO(Long id, String userId, String operation, String model, Boolean success, Integer totalTokens,
    Long durationMs, Instant createdAt) {
}
