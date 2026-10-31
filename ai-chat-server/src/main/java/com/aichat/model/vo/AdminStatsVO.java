package com.aichat.model.vo;

import java.util.List;

/** 管理端汇总统计。 */
public record AdminStatsVO(Long users, Long trees, Long calls, List<AiUsageVO> recentUsage) {
}
