package com.aichat.model.vo;

/** 供下游系统消费的导出协议。 */
public record TreeExportVO(String schemaVersion, String topic, String description, ExportNodeVO root) {
}
