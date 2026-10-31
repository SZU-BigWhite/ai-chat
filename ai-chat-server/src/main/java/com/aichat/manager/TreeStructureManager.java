package com.aichat.manager;

import com.aichat.config.TreeProperties;
import com.aichat.exception.ApiException;
import com.aichat.model.domain.KnowledgeNode;
import com.aichat.model.dto.CandidateDTO;
import com.aichat.model.entity.KnowledgeTreeDO;
import com.aichat.model.enums.NodeStatusEnum;
import com.aichat.model.vo.TreeVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 树遍历、校验与 JSON 编解码；不访问数据库或外部模型。 */
@Component
public class TreeStructureManager {
    private final ObjectMapper mapper;
    private final TreeProperties properties;

    public TreeStructureManager(ObjectMapper mapper, TreeProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    public TreeVO view(KnowledgeTreeDO tree) {
        return new TreeVO(tree.getId(), tree.getTitle(), tree.getDescription(), tree.getTopicId(), tree.getRevision(),
            tree.getUpdatedAt(), root(tree));
    }

    public KnowledgeNode root(KnowledgeTreeDO tree) {
        try {
            return mapper.readValue(tree.getRootJson(), KnowledgeNode.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid stored tree", exception);
        }
    }

    public KnowledgeNode find(KnowledgeNode root, String nodeId) {
        return flatten(root).stream().filter(n -> n.getId().equals(nodeId)).findFirst()
            .orElseThrow(ApiException::missing);
    }

    public List<KnowledgeNode> path(KnowledgeNode root, String nodeId) {
        List<KnowledgeNode> path = new ArrayList<>();
        if (!pathTo(root, nodeId, path)) {
            throw ApiException.missing();
        }
        return path;
    }

    private boolean pathTo(KnowledgeNode node, String id, List<KnowledgeNode> path) {
        path.add(node);
        if (node.getId().equals(id)) {
            return true;
        }
        for (KnowledgeNode child : node.getChildren()) {
            if (pathTo(child, id, path)) {
                return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    public List<KnowledgeNode> flatten(KnowledgeNode root) {
        List<KnowledgeNode> all = new ArrayList<>();
        all.add(root);
        for (KnowledgeNode child : root.getChildren()) {
            all.addAll(flatten(child));
        }
        return all;
    }

    public KnowledgeNode parent(KnowledgeNode root, String nodeId) {
        for (KnowledgeNode child : root.getChildren()) {
            if (child.getId().equals(nodeId)) {
                return root;
            }
            KnowledgeNode found = parent(child, nodeId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public void markReview(KnowledgeNode node) {
        node.setStatus(NodeStatusEnum.REVIEW.name());
        node.getChildren().forEach(this::markReview);
    }

    public void regenerateIds(KnowledgeNode node) {
        node.setId(id());
        node.getChildren().forEach(this::regenerateIds);
    }

    /** 客户端版本检查与 JPA 乐观锁配合，避免多个页面相互覆盖。 */
    public void checkRevision(KnowledgeTreeDO tree, long revision) {
        if (tree.getRevision() != revision) {
            throw ApiException.conflict();
        }
    }

    public void ensureCapacity(KnowledgeNode root, String nodeId, int added) {
        if (flatten(root).size() + added > properties.getMaxNodes()) {
            throw ApiException.bad("单棵树最多 " + properties.getMaxNodes() + " 个节点，请按主题拆分为多棵树");
        }
        if (path(root, nodeId).size() >= properties.getMaxDepth()) {
            throw ApiException.bad("已达到 " + properties.getMaxDepth() + " 层深度上限");
        }
    }

    public void ensureUnique(KnowledgeNode node, List<CandidateDTO> children) {
        Set<String> names = new HashSet<>();
        node.getChildren().forEach(child -> names.add(child.getName().toLowerCase(Locale.ROOT)));
        for (CandidateDTO child : children) {
            if (!names.add(child.name().toLowerCase(Locale.ROOT))) {
                throw ApiException.bad("同层存在重复名称，请修改后采纳");
            }
        }
    }

    public String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize knowledge data", exception);
        }
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

}
