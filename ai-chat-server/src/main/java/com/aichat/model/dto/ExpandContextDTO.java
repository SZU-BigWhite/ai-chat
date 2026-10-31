package com.aichat.model.dto;

import java.util.List;

/** 发给模型的节点上下文，字段与提示词协议保持一致。 */
public record ExpandContextDTO(String topic, String scope, List<CandidateDTO> path, List<CandidateDTO> existingChildren,
    String instruction, Integer count) {
}
