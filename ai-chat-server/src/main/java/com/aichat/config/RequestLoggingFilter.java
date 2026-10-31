package com.aichat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** 为每次 API 请求分配编号，让浏览器、业务日志和接口耗时能够相互对应。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        // 编号由服务端生成，不接受外部任意文本进入日志上下文。
        String requestId = UUID.randomUUID().toString();
        String previous = MDC.get("requestId");
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        long started = System.nanoTime();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            // 只记录匹配到的路由模板，不记录查询参数、请求体或认证头。
            Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            int status = failed ? HttpStatus.INTERNAL_SERVER_ERROR.value() : response.getStatus();
            long durationMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            String pattern = route == null ? "unmapped" : route.toString();
            if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                LOGGER.error("HTTP method={} route={} status={} durationMs={}", request.getMethod(), pattern, status,
                    durationMs);
            } else if (status >= HttpStatus.BAD_REQUEST.value()) {
                LOGGER.warn("HTTP method={} route={} status={} durationMs={}", request.getMethod(), pattern, status,
                    durationMs);
            } else if ("/api/health".equals(pattern)) {
                LOGGER.debug("HTTP health status={} durationMs={}", status, durationMs);
            } else {
                LOGGER.info("HTTP method={} route={} status={} durationMs={}", request.getMethod(), pattern, status,
                    durationMs);
            }
            // Servlet 工作线程会被复用，必须恢复上下文，避免请求编号串到下一次请求。
            if (previous == null) {
                MDC.remove("requestId");
            } else {
                MDC.put("requestId", previous);
            }
        }
    }
}
