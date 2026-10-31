package com.aichat.service.impl;

import com.aichat.config.AiLimitProperties;
import com.aichat.exception.ApiException;
import com.aichat.manager.DeepSeekManager;
import com.aichat.manager.TreeStructureManager;
import com.aichat.model.domain.KnowledgeNode;
import com.aichat.model.dto.CandidateDTO;
import com.aichat.model.dto.ExpandContextDTO;
import com.aichat.model.dto.ExpandNodeDTO;
import com.aichat.model.dto.ExtractTopicDTO;
import com.aichat.model.entity.KnowledgeTreeDO;
import com.aichat.model.enums.AiOperationEnum;
import com.aichat.model.vo.SuggestionVO;
import com.aichat.service.AiService;
import com.aichat.service.TreeService;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 组织主题提取和单节点拆分；模型通信交给独立 Manager。 */
@Service
public class AiServiceImpl implements AiService {
    private final DeepSeekManager deepSeekManager;
    private final TreeService treeService;
    private final TreeStructureManager structure;
    private final AiLimitProperties properties;

    public AiServiceImpl(DeepSeekManager deepSeekManager, TreeService treeService, TreeStructureManager structure,
        AiLimitProperties properties) {
        this.deepSeekManager = deepSeekManager;
        this.treeService = treeService;
        this.structure = structure;
        this.properties = properties;
    }

    @Override
    public boolean configured() {
        return deepSeekManager.isConfigured();
    }

    @Override
    public List<CandidateDTO> extract(String userId, ExtractTopicDTO request) {
        return deepSeekManager.generate(userId, AiOperationEnum.EXTRACT, request.text(),
            properties.getExtractCandidates());
    }

    /** 生成的内容仅保存为建议，不直接改动知识树。 */
    @Override
    public SuggestionVO expand(String userId, String treeId, String nodeId, ExpandNodeDTO request) {
        KnowledgeTreeDO tree = treeService.getOwnedTree(userId, treeId);
        structure.checkRevision(tree, request.revision());
        KnowledgeNode root = structure.root(tree);
        KnowledgeNode node = structure.find(root, nodeId);
        List<KnowledgeNode> path = structure.path(root, nodeId);
        ExpandContextDTO context = new ExpandContextDTO(tree.getTitle(), tree.getDescription(),
            path.stream().map(this::toCandidate).toList(), node.getChildren().stream().map(this::toCandidate).toList(),
            request.instruction() == null ? "" : request.instruction(), request.count());
        List<CandidateDTO> candidates =
            deepSeekManager.generate(userId, AiOperationEnum.EXPAND, structure.json(context), request.count());
        Set<String> existingNames = new HashSet<>();
        node.getChildren().forEach(child -> existingNames.add(child.getName().toLowerCase(Locale.ROOT)));
        path.forEach(ancestor -> existingNames.add(ancestor.getName().toLowerCase(Locale.ROOT)));
        List<CandidateDTO> children = candidates.stream()
            .filter(candidate -> existingNames.add(candidate.name().toLowerCase(Locale.ROOT))).toList();
        if (children.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "本轮没有产生新的有效子节点，请调整拆分要求，或将当前节点保留为叶子");
        }
        return treeService.saveSuggestion(userId, treeId, nodeId, request.revision(), request.instruction(), children);
    }

    private CandidateDTO toCandidate(KnowledgeNode node) {
        return new CandidateDTO(node.getName(), node.getDescription());
    }
}
