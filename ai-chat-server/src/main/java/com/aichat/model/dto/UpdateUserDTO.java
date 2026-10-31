package com.aichat.model.dto;

import jakarta.validation.constraints.NotNull;

/** 管理员维护用户启用状态。 */
public record UpdateUserDTO(@NotNull Boolean enabled) {
}
