package com.aichat.model.dto;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 登录凭证，不得写入日志。 */
public record LoginDTO(@NotBlank @Email @Size(max = BusinessConstants.MAX_EMAIL_LENGTH) String email,
    @NotBlank @Size(max = BusinessConstants.MAX_PASSWORD_LENGTH) String password) {
    @Override
    public String toString() {
        return "LoginDTO[credentials=REDACTED]";
    }
}
