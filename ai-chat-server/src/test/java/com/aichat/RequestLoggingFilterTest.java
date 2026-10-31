package com.aichat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aichat.config.RequestLoggingFilter;
import com.aichat.exception.SafeLog;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

/** 验证日志可用于关联请求，同时不泄露表单、令牌、查询参数和外部异常消息。 */
class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger logger = (Logger)LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void capture() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void cleanup() {
        logger.detachAppender(appender);
        appender.stop();
        MDC.clear();
    }

    @Test
    void correlatesRequestsWithoutLoggingSensitiveInput() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/auth/login");
        request.setQueryString("token=query-secret");
        request.addHeader("Authorization", "Bearer header-secret");
        request.addHeader("X-Request-Id", "untrusted-request-id");
        request.setContent("{\"password\":\"body-secret\"}".getBytes(StandardCharsets.UTF_8));

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("requestId")).isEqualTo(response.getHeader("X-Request-Id"));
            response.setStatus(401);
        });

        assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
        assertThat(MDC.get("requestId")).isNull();
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage())
            .contains("POST", "/api/auth/login", "status=401", "durationMs=")
            .doesNotContain("query-secret", "header-secret", "body-secret", "untrusted-request-id");
    }

    @Test
    void restoresContextAndReportsUnhandledFailures() {
        MDC.put("requestId", "outer-context");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/trees");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("private-error-content");
        })).isInstanceOf(ServletException.class);

        assertThat(MDC.get("requestId")).isEqualTo("outer-context");
        assertThat(appender.list.get(0).getFormattedMessage()).contains("status=500")
            .doesNotContain("private-error-content");
    }

    @Test
    void exceptionSummaryKeepsLocationsButOmitsMessages() {
        Exception error =
            new IllegalStateException("private-sql-values", new IllegalArgumentException("private-model-response"));
        assertThat(SafeLog.describe(error))
            .contains("IllegalStateException", "IllegalArgumentException", "RequestLoggingFilterTest.java")
            .doesNotContain("private-sql-values", "private-model-response");
    }
}
