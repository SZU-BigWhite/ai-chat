package com.aichat.model.vo;

/** 主题展示信息。 */
public record TopicVO(String id, String name, String description, String icon, String color, Integer sortOrder,
    Boolean enabled) {
}
