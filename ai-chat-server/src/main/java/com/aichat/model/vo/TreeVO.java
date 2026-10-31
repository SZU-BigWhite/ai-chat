package com.aichat.model.vo;

import com.aichat.model.domain.KnowledgeNode;
import java.time.Instant;

/** 可编辑的知识树及其版本。 */
public record TreeVO(String id, String title, String description, String topicId, Long revision, Instant updatedAt,
    KnowledgeNode root) {
}
