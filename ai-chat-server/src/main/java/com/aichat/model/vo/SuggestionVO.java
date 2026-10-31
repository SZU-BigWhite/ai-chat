package com.aichat.model.vo;

import com.aichat.model.dto.CandidateDTO;
import java.time.Instant;
import java.util.List;

/** 绑定树版本的待确认拆分建议。 */
public record SuggestionVO(String id, String nodeId, Long treeRevision, String instruction, List<CandidateDTO> children,
    Instant createdAt) {
}
