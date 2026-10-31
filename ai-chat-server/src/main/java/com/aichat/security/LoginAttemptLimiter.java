package com.aichat.security;

import com.aichat.config.AuthProperties;
import com.aichat.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 单实例登录限流；窗口与次数从配置读取，多实例部署需替换为共享存储。 */
@Component
public class LoginAttemptLimiter {
    private final AuthProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** 单个来源地址的固定时间窗口计数。 */
    private record AttemptWindow(Instant expiresAt, long count) {
    }

    public void check(String remoteAddress) {
        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        AttemptWindow window = attempts.compute(remoteAddress, (address, previous) -> {
            if (previous == null || !now.isBefore(previous.expiresAt())) {
                return new AttemptWindow(now.plus(properties.getLoginWindow()), 1);
            }
            // 达到上限后不继续累加，避免持续请求造成计数溢出。
            return new AttemptWindow(previous.expiresAt(),
                Math.min(previous.count(), properties.getLoginMaxAttempts()) + 1);
        });
        if (window.count() > properties.getLoginMaxAttempts()) {
            long retrySeconds = Math.max(1, Duration.between(now, window.expiresAt()).toSeconds());
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁，请 " + retrySeconds + " 秒后重试");
        }
    }
}
