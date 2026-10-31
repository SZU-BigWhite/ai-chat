package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 注册输入，密码仅用于校验及哈希计算。 */
public record RegisterDTO(@NotBlank @Email @Size(max = BusinessConstants.MAX_EMAIL_LENGTH) String email,
    @NotBlank @Size(min = BusinessConstants.MIN_PASSWORD_LENGTH,
        max = BusinessConstants.MAX_PASSWORD_LENGTH) String password,
    @NotBlank @Size(max = BusinessConstants.MAX_USER_NAME_LENGTH) String name) {
    @Override
    public String toString() {
        return "RegisterDTO[credentials=REDACTED]";
    }
}
