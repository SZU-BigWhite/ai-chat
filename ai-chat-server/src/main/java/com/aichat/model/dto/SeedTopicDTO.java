package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 预置主题文件的数据契约，加载时校验，避免错误数据进入数据库。 */
public record SeedTopicDTO(
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]*") @Size(max = BusinessConstants.MAX_NODE_NAME_LENGTH) String id,
    @NotBlank @Size(max = BusinessConstants.MAX_NODE_NAME_LENGTH) String name,
    @NotBlank @Size(max = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH) String description,
    @NotBlank @Size(max = BusinessConstants.MAX_TOPIC_ICON_LENGTH) String icon,
    @NotNull @Pattern(regexp = BusinessConstants.HEX_COLOR_PATTERN) String color, @NotNull Integer sortOrder) {
}
