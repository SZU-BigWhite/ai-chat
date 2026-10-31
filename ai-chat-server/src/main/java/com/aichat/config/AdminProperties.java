package com.aichat.config;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 管理员初始化凭证与统计显示配置；禁止记录本配置对象中的凭证。 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {
    @NotNull
    private String email = "";

    @NotNull
    private String password = "";

    @NotBlank
    @Size(max = BusinessConstants.MAX_USER_NAME_LENGTH)
    private String displayName = "管理员";

    @Min(1)
    @Max(1000)
    private int recentUsageLimit = 100;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getRecentUsageLimit() {
        return recentUsageLimit;
    }

    public void setRecentUsageLimit(int recentUsageLimit) {
        this.recentUsageLimit = recentUsageLimit;
    }

}
