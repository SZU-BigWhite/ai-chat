package com.aichat.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 某个知识树版本的完整导出内容，历史快照不随后续编辑更新。 */
@Entity
@Table(name = "export_snapshot")
public class ExportSnapshotDO {
    @Id
    private String id;
    private String treeId;
    private Long treeRevision = 0L;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTreeId() {
        return treeId;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    public Long getTreeRevision() {
        return treeRevision;
    }

    public void setTreeRevision(Long treeRevision) {
        this.treeRevision = treeRevision;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
