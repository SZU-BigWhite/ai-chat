package com.aichat.model.domain;

import com.aichat.model.enums.NodeStatusEnum;
import java.util.ArrayList;
import java.util.List;

/** 可编辑的树节点；JSON 字段保持与已保存的知识树兼容。 */
public class KnowledgeNode {
    private String id;
    private String name;
    private String description;
    private String status = NodeStatusEnum.DRAFT.name();
    private List<KnowledgeNode> children = new ArrayList<>();

    public KnowledgeNode() {}

    public KnowledgeNode(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<KnowledgeNode> getChildren() {
        return children;
    }

    public void setChildren(List<KnowledgeNode> children) {
        this.children = children;
    }
}
