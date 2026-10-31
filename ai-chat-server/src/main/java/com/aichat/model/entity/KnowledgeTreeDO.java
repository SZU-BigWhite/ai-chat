package com.aichat.model.entity;

import com.aichat.constant.BusinessConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** 一条记录保存一棵知识树；节点存储为 JSON，revision 用于并发写入保护。 */
@Entity
@Table(name = "knowledge_tree", indexes = @Index(name = "idx_tree_owner", columnList = "ownerId"))
public class KnowledgeTreeDO {
    @Id
    private String id;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false, length = BusinessConstants.MAX_NODE_NAME_LENGTH)
    private String title;

    @Column(length = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH)
    private String description;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String rootJson;

    private String topicId;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    // 数据库更新时自动递增，拒绝基于旧版本的并发覆盖。
    @Version
    private Long revision = 0L;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRootJson() {
        return rootJson;
    }

    public void setRootJson(String rootJson) {
        this.rootJson = rootJson;
    }

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }
}
