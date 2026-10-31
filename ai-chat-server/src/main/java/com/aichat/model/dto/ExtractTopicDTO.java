package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 待分析的主题描述。 */
public record ExtractTopicDTO(@NotBlank @Size(max = BusinessConstants.MAX_TOPIC_INPUT_LENGTH) String text) {
}
