package com.aichat.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 认证策略配置；持续时间必须为正，避免失效会话和无限限流窗口。 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    @NotNull
    @DurationMin(seconds = 1)
    private Duration sessionTtl = Duration.ofDays(7);

    @NotNull
    @DurationMin(seconds = 1)
    private Duration loginWindow = Duration.ofMinutes(15);

    @Min(1)
    private int loginMaxAttempts = 30;

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public Duration getLoginWindow() {
        return loginWindow;
    }

    public void setLoginWindow(Duration loginWindow) {
        this.loginWindow = loginWindow;
    }

    public int getLoginMaxAttempts() {
        return loginMaxAttempts;
    }

    public void setLoginMaxAttempts(int loginMaxAttempts) {
        this.loginMaxAttempts = loginMaxAttempts;
    }

}
