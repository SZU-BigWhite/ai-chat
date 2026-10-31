package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** AI 候选名称与知识范围，也用于用户确认后的输入。 */
public record CandidateDTO(@NotBlank @Size(max = BusinessConstants.MAX_NODE_NAME_LENGTH) String name,
    @NotBlank @Size(max = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH) String description) {
}
