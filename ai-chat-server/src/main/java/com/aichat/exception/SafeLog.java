package com.aichat.exception;

import java.util.Arrays;
import java.util.stream.Collectors;

/** 保留异常类型和应用代码位置，不输出可能携带 SQL 参数或模型原文的异常消息。 */
public final class SafeLog {
    private static final int MAX_CAUSE_DEPTH = 5;
    private static final int MAX_SUMMARY_FRAMES = 6;

    private SafeLog() {}

    public static String describe(Throwable error) {
        StringBuilder summary = new StringBuilder();
        Throwable current = error;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (depth > 0) {
                summary.append(" <- ");
            }
            summary.append(current.getClass().getSimpleName());
            String frames =
                Arrays.stream(current.getStackTrace()).filter(frame -> frame.getClassName().startsWith("com.aichat."))
                    .limit(MAX_SUMMARY_FRAMES).map(StackTraceElement::toString).collect(Collectors.joining("; "));
            if (!frames.isEmpty()) {
                summary.append(" [").append(frames).append("]");
            }
            current = current.getCause();
        }
        return summary.toString();
    }

    /** 保留完整调用栈，异常消息仅使用类型名，避免外部响应或 SQL 参数泄露。 */
    public static Throwable forLogging(Throwable error) {
        return sanitize(error, 0);
    }

    private static Throwable sanitize(Throwable error, int depth) {
        if (error == null || depth >= MAX_CAUSE_DEPTH) {
            return null;
        }
        RuntimeException safe = new RuntimeException(error.getClass().getName(), sanitize(error.getCause(), depth + 1));
        safe.setStackTrace(error.getStackTrace());
        for (Throwable suppressed : error.getSuppressed()) {
            Throwable sanitized = sanitize(suppressed, depth + 1);
            if (sanitized != null) {
                safe.addSuppressed(sanitized);
            }
        }
        return safe;
    }
}
