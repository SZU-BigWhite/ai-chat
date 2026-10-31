package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 手动添加子节点的输入。 */
public record AddNodeDTO(@NotNull Long revision,
    @NotBlank @Size(max = BusinessConstants.MAX_NODE_NAME_LENGTH) String name,
    @Size(max = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH) String description) {
}
