package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 管理员维护预置主题的输入。 */
public record UpdateTopicDTO(@NotBlank @Size(max = BusinessConstants.MAX_NODE_NAME_LENGTH) String name,
    @NotBlank @Size(max = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH) String description,
    @NotBlank @Size(max = BusinessConstants.MAX_TOPIC_ICON_LENGTH) String icon,
    @Pattern(regexp = BusinessConstants.HEX_COLOR_PATTERN) String color, Integer sortOrder, Boolean enabled) {
    public UpdateTopicDTO {
        sortOrder = sortOrder == null ? 0 : sortOrder;
        enabled = Boolean.TRUE.equals(enabled);
    }
}
