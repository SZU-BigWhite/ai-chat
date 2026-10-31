package com.aichat.model.entity;

import com.aichat.constant.BusinessConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** 与知识树版本绑定的 AI 建议，采纳前不会改变正式节点。 */
@Entity
@Table(name = "suggestion", indexes = @Index(name = "idx_suggestion_tree", columnList = "treeId"))
public class SuggestionDO {
    @Id
    private String id;
    private String treeId;
    private String nodeId;
    private Long treeRevision = 0L;

    @Column(length = BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH)
    private String instruction;

    @Column(columnDefinition = "TEXT")
    private String childrenJson;

    private Boolean accepted = false;
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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Long getTreeRevision() {
        return treeRevision;
    }

    public void setTreeRevision(Long treeRevision) {
        this.treeRevision = treeRevision;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getChildrenJson() {
        return childrenJson;
    }

    public void setChildrenJson(String childrenJson) {
        this.childrenJson = childrenJson;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
