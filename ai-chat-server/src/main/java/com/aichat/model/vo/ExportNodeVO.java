package com.aichat.model.vo;

import java.util.List;

/** 对外知识节点，不含内部编辑状态。 */
public record ExportNodeVO(String id, String name, String description, List<ExportNodeVO> children) {
}
