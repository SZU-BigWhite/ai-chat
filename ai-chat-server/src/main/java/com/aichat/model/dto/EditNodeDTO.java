package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import com.aichat.model.enums.NodeStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 节点修改与客户端版本。 */
public record EditNodeDTO(@NotNull Long revision,
    @NotBlank @Size(max = BusinessConstants.MAX_NODE_NAME_LENGTH) String name,
    @Size(max = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH) String description, NodeStatusEnum status) {
}
