package com.aichat.model.vo;

import java.time.Instant;

/** 历史导出版本元信息。 */
public record ExportVersionVO(String id, Long treeRevision, Instant createdAt) {
}
