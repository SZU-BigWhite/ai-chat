package com.aichat.config;

import com.aichat.constant.BusinessConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.ZoneId;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** AI 额度、超时与候选数量配置；非法值在启动时拒绝。 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiLimitProperties {
    @Min(1)
    private int dailyLimit = 100;

    @NotNull
    private ZoneId quotaZone = ZoneId.of("Asia/Shanghai");

    @NotNull
    @DurationMin(millis = 1)
    private Duration connectTimeout = Duration.ofSeconds(15);

    @NotNull
    @DurationMin(millis = 1)
    private Duration readTimeout = Duration.ofSeconds(120);

    @Min(1)
    @Max(BusinessConstants.MAX_AI_CANDIDATES)
    private int extractCandidates = 3;

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public ZoneId getQuotaZone() {
        return quotaZone;
    }

    public void setQuotaZone(ZoneId quotaZone) {
        this.quotaZone = quotaZone;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getExtractCandidates() {
        return extractCandidates;
    }

    public void setExtractCandidates(int extractCandidates) {
        this.extractCandidates = extractCandidates;
    }

}
