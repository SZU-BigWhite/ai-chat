package com.aichat.model.entity;

import com.aichat.constant.BusinessConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 可由管理员维护的预置主题入口；停用后不再出现在主题列表。 */
@Entity
@Table(name = "topic")
public class TopicDO {
    @Id
    private String id;

    @Column(nullable = false, length = BusinessConstants.MAX_NODE_NAME_LENGTH)
    private String name;

    @Column(length = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH)
    private String description;

    private String icon;
    private String color;
    private Integer sortOrder = 0;
    private Boolean enabled = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
