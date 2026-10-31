package com.aichat.model.vo;

import java.time.Instant;

/** 知识树列表中的汇总信息。 */
public record TreeSummaryVO(String id, String title, String description, Long revision, Instant updatedAt,
    Integer nodeCount, Integer confirmedCount) {
}
