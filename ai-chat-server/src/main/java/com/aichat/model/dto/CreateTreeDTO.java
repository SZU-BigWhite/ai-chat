package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 创建根主题的输入。 */
public record CreateTreeDTO(@NotBlank @Size(max = BusinessConstants.MAX_NODE_NAME_LENGTH) String name,
    @Size(max = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH) String description, String topicId) {
}
