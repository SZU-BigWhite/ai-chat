package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 生成下一层候选所需的参数。 */
public record ExpandNodeDTO(@NotNull Long revision,
    @Size(max = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH) String instruction,
    @NotNull @Min(BusinessConstants.MIN_AI_CANDIDATES) @Max(BusinessConstants.MAX_AI_CANDIDATES) Integer count) {
}
