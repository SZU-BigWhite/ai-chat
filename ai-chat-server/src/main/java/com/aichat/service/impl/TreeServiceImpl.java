package com.aichat.service.impl;

import com.aichat.constant.BusinessConstants;
import com.aichat.exception.ApiException;
import com.aichat.manager.TreeStructureManager;
import com.aichat.model.domain.KnowledgeNode;
import com.aichat.model.dto.AcceptSuggestionDTO;
import com.aichat.model.dto.AddNodeDTO;
import com.aichat.model.dto.CandidateDTO;
import com.aichat.model.dto.CreateTreeDTO;
import com.aichat.model.dto.EditNodeDTO;
import com.aichat.model.entity.ExportSnapshotDO;
import com.aichat.model.entity.KnowledgeTreeDO;
import com.aichat.model.entity.SuggestionDO;
import com.aichat.model.enums.NodeStatusEnum;
import com.aichat.model.vo.ExportContentVO;
import com.aichat.model.vo.ExportNodeVO;
import com.aichat.model.vo.ExportVersionVO;
import com.aichat.model.vo.SuggestionVO;
import com.aichat.model.vo.TreeExportVO;
import com.aichat.model.vo.TreeSummaryVO;
import com.aichat.model.vo.TreeVO;
import com.aichat.repository.ExportSnapshotRepository;
import com.aichat.repository.KnowledgeTreeRepository;
import com.aichat.repository.SuggestionRepository;
import com.aichat.repository.TopicRepository;
import com.aichat.service.TreeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 维护用户私有知识树、版本校验、候选建议和不可变导出快照。 */
@Service
@Transactional(readOnly = true)
public class TreeServiceImpl implements TreeService {
    private static final String COPY_NAME_SUFFIX = " · 副本";
    private static final Logger LOGGER = LoggerFactory.getLogger(TreeServiceImpl.class);
    private final KnowledgeTreeRepository treeRepository;
    private final TopicRepository topicRepository;
    private final SuggestionRepository suggestionRepository;
    private final ExportSnapshotRepository exportRepository;
    private final ObjectMapper mapper;
    private final TreeStructureManager treeStructureManager;

    public TreeServiceImpl(KnowledgeTreeRepository treeRepository, TopicRepository topicRepository,
        SuggestionRepository suggestionRepository, ExportSnapshotRepository exportRepository, ObjectMapper mapper,
        TreeStructureManager treeStructureManager) {
        this.treeRepository = treeRepository;
        this.topicRepository = topicRepository;
        this.suggestionRepository = suggestionRepository;
        this.exportRepository = exportRepository;
        this.mapper = mapper;
        this.treeStructureManager = treeStructureManager;
    }

    /** 所有读写统一校验所属用户，其他用户的资源按不存在处理。 */
    @Override
    public KnowledgeTreeDO getOwnedTree(String ownerId, String id) {
        return treeRepository.findByIdAndOwnerId(id, ownerId).orElseThrow(ApiException::missing);
    }

    @Override
    public TreeVO getTree(String ownerId, String id) {
        return treeStructureManager.view(getOwnedTree(ownerId, id));
    }

    @Override
    public List<TreeSummaryVO> listTrees(String ownerId) {
        return treeRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(tree -> {
            List<KnowledgeNode> nodes = treeStructureManager.flatten(treeStructureManager.root(tree));
            return new TreeSummaryVO(tree.getId(), tree.getTitle(), tree.getDescription(), tree.getRevision(),
                tree.getUpdatedAt(), nodes.size(),
                (int)nodes.stream().filter(n -> NodeStatusEnum.CONFIRMED.name().equals(n.getStatus())).count());
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TreeVO createTree(String ownerId, CreateTreeDTO request) {
        if (request.topicId() != null && !request.topicId().isBlank()) {
            topicRepository.findById(request.topicId()).filter(tree -> tree.getEnabled())
                .orElseThrow(ApiException::missing);
        }
        KnowledgeTreeDO tree = new KnowledgeTreeDO();
        tree.setId(id());
        tree.setOwnerId(ownerId);
        tree.setTopicId(request.topicId());
        KnowledgeNode root = new KnowledgeNode(id(), request.name().trim(), clean(request.description()));
        return save(tree, root);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TreeVO updateNode(String ownerId, String treeId, String nodeId, EditNodeDTO request) {
        KnowledgeTreeDO tree = getOwnedTree(ownerId, treeId);
        treeStructureManager.checkRevision(tree, request.revision());
        KnowledgeNode root = treeStructureManager.root(tree);
        KnowledgeNode node = treeStructureManager.find(root, nodeId);
        String name = request.name().trim();
        String description = clean(request.description());
        KnowledgeNode parent = treeStructureManager.parent(root, nodeId);
        if (parent != null && parent.getChildren().stream()
            .anyMatch(c -> !c.getId().equals(nodeId) && c.getName().equalsIgnoreCase(name))) {
            throw ApiException.bad("同层已有同名节点");
        }
        boolean changed = !name.equals(node.getName()) || !description.equals(node.getDescription());
        node.setName(name);
        node.setDescription(description);
        // 父节点范围变化会影响下属知识的适用性，整个子树都需要用户重新复核。
        if (changed) {
            for (KnowledgeNode child : node.getChildren()) {
                treeStructureManager.markReview(child);
            }
        }
        if (request.status() != null) {
            node.setStatus(request.status().name());
        }
        return save(tree, root);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TreeVO addNode(String ownerId, String treeId, String nodeId, AddNodeDTO request) {
        KnowledgeTreeDO tree = getOwnedTree(ownerId, treeId);
        treeStructureManager.checkRevision(tree, request.revision());
        KnowledgeNode root = treeStructureManager.root(tree);
        KnowledgeNode node = treeStructureManager.find(root, nodeId);
        treeStructureManager.ensureCapacity(root, nodeId, 1);
        treeStructureManager.ensureUnique(node,
            List.of(new CandidateDTO(request.name().trim(), clean(request.description()))));
        node.getChildren().add(new KnowledgeNode(id(), request.name().trim(), clean(request.description())));
        node.setStatus(NodeStatusEnum.DRAFT.name());
        return save(tree, root);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TreeVO deleteNode(String ownerId, String treeId, String nodeId, long revision) {
        KnowledgeTreeDO tree = getOwnedTree(ownerId, treeId);
        treeStructureManager.checkRevision(tree, revision);
        KnowledgeNode root = treeStructureManager.root(tree);
        if (root.getId().equals(nodeId)) {
            throw ApiException.bad("根节点不能单独删除，请删除整棵知识树");
        }
        KnowledgeNode parent = treeStructureManager.parent(root, nodeId);
        if (parent == null) {
            throw ApiException.missing();
        }
        parent.getChildren().removeIf(n -> n.getId().equals(nodeId));
        parent.setStatus(NodeStatusEnum.DRAFT.name());
        return save(tree, root);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteTree(String ownerId, String treeId, long revision) {
        KnowledgeTreeDO tree = getOwnedTree(ownerId, treeId);
        treeStructureManager.checkRevision(tree, revision);
        suggestionRepository.deleteByTreeId(treeId);
        exportRepository.deleteByTreeId(treeId);
        treeRepository.delete(tree);
        LOGGER.info("Tree deletion scheduled treeId={} userId={} revision={}", treeId, ownerId, revision);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TreeVO copyTree(String ownerId, String treeId) {
        KnowledgeTreeDO source = getOwnedTree(ownerId, treeId);
        KnowledgeNode root = treeStructureManager.root(source);
        treeStructureManager.regenerateIds(root);
        root.setName((root.getName().length() > BusinessConstants.MAX_NODE_NAME_LENGTH - COPY_NAME_SUFFIX.length()
            ? root.getName().substring(0, BusinessConstants.MAX_NODE_NAME_LENGTH - COPY_NAME_SUFFIX.length())
            : root.getName()) + COPY_NAME_SUFFIX);
        KnowledgeTreeDO target = new KnowledgeTreeDO();
        target.setId(id());
        target.setOwnerId(ownerId);
        target.setTopicId(source.getTopicId());
        return save(target, root);
    }

    @Override
    public List<SuggestionVO> listPendingSuggestions(String ownerId, String treeId) {
        KnowledgeTreeDO tree = getOwnedTree(ownerId, treeId);
        return suggestionRepository.findByTreeIdAndAcceptedFalseOrderByCreatedAtDesc(treeId).stream()
            .filter(suggestion -> Objects.equals(suggestion.getTreeRevision(), tree.getRevision()))
            .map(this::suggestionView).toList();
    }

    /** AI 调用可能耗时较长，落库前再次检查版本，防止保存过期上下文的建议。 */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SuggestionVO saveSuggestion(String ownerId, String treeId, String nodeId, long revision, String instruction,
        List<CandidateDTO> children) {
        KnowledgeTreeDO tree = getOwnedTree(ownerId, treeId);
        treeStructureManager.checkRevision(tree, revision);
        treeStructureManager.find(treeStructureManager.root(tree), nodeId);
        SuggestionDO suggestion = new SuggestionDO();
        suggestion.setId(id());
        suggestion.setTreeId(treeId);
        suggestion.setNodeId(nodeId);
        suggestion.setTreeRevision(revision);
        suggestion.setInstruction(clean(instruction));
        suggestion.setChildrenJson(treeStructureManager.json(children));
        return suggestionView(suggestionRepository.save(suggestion));
    }

    /** 只在原知识树版本仍有效时采纳；结构变更与建议状态在同一事务提交。 */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public TreeVO acceptSuggestion(String ownerId, String treeId, String suggestionId, AcceptSuggestionDTO request) {
        KnowledgeTreeDO tree = getOwnedTree(ownerId, treeId);
        treeStructureManager.checkRevision(tree, request.revision());
        SuggestionDO suggestion = suggestionRepository.findById(suggestionId)
            .filter(item -> item.getTreeId().equals(treeId)).orElseThrow(ApiException::missing);
        if (suggestion.getAccepted() || !Objects.equals(suggestion.getTreeRevision(), tree.getRevision())) {
            throw ApiException.conflict();
        }
        KnowledgeNode root = treeStructureManager.root(tree);
        KnowledgeNode node = treeStructureManager.find(root, suggestion.getNodeId());
        treeStructureManager.ensureCapacity(root, node.getId(), request.children().size());
        List<CandidateDTO> clean =
            request.children().stream().map(c -> new CandidateDTO(c.name().trim(), c.description().trim())).toList();
        treeStructureManager.ensureUnique(node, clean);
        clean.forEach(c -> node.getChildren().add(new KnowledgeNode(id(), c.name(), c.description())));
        node.setStatus(NodeStatusEnum.DRAFT.name());
        suggestion.setAccepted(true);
        suggestionRepository.save(suggestion);
        LOGGER.info("SuggestionDO acceptance scheduled treeId={} nodeId={} suggestionId={} addedNodes={}", treeId,
            suggestion.getNodeId(), suggestionId, clean.size());
        return save(tree, root);
    }

    @Override
    public TreeExportVO getExportPreview(String ownerId, String treeId) {
        return exportContent(getOwnedTree(ownerId, treeId));
    }

    /** 固化当前 JSON 快照，后续编辑不会改变历史导出版本。 */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ExportContentVO createExport(String ownerId, String treeId, long revision) {
        KnowledgeTreeDO tree = getOwnedTree(ownerId, treeId);
        treeStructureManager.checkRevision(tree, revision);
        ExportSnapshotDO snapshot = new ExportSnapshotDO();
        snapshot.setId(id());
        snapshot.setTreeId(treeId);
        snapshot.setTreeRevision(tree.getRevision());
        snapshot.setContent(treeStructureManager.json(exportContent(tree)));
        ExportSnapshotDO saved = exportRepository.save(snapshot);
        LOGGER.info("Export snapshot persisted treeId={} revision={} exportId={}", treeId, tree.getRevision(),
            saved.getId());
        return new ExportContentVO(saved.getContent());
    }

    @Override
    public List<ExportVersionVO> listExportVersions(String ownerId, String treeId) {
        getOwnedTree(ownerId, treeId);
        return exportRepository.findByTreeIdOrderByCreatedAtDesc(treeId).stream()
            .map(snapshot -> new ExportVersionVO(snapshot.getId(), snapshot.getTreeRevision(), snapshot.getCreatedAt()))
            .toList();
    }

    @Override
    public ExportContentVO getExportVersion(String ownerId, String treeId, String versionId) {
        getOwnedTree(ownerId, treeId);
        ExportSnapshotDO snapshot = exportRepository.findById(versionId).filter(item -> item.getTreeId().equals(treeId))
            .orElseThrow(ApiException::missing);
        return new ExportContentVO(snapshot.getContent());
    }

    private TreeExportVO exportContent(KnowledgeTreeDO tree) {
        return new TreeExportVO(BusinessConstants.EXPORT_SCHEMA_VERSION, tree.getTitle(), tree.getDescription(),
            exportNode(treeStructureManager.root(tree)));
    }

    /** 对外只导出知识结构，省略编辑状态和用户信息。 */
    private ExportNodeVO exportNode(KnowledgeNode node) {
        return new ExportNodeVO(node.getId(), node.getName(), node.getDescription(),
            node.getChildren().stream().map(this::exportNode).toList());
    }

    private TreeVO save(KnowledgeTreeDO tree, KnowledgeNode root) {
        tree.setTitle(root.getName());
        tree.setDescription(root.getDescription());
        tree.setRootJson(treeStructureManager.json(root));
        tree.setUpdatedAt(Instant.now());
        KnowledgeTreeDO saved = treeRepository.saveAndFlush(tree);
        LOGGER.info("Tree write flushed treeId={} userId={} revision={}", saved.getId(), saved.getOwnerId(),
            saved.getRevision());
        return treeStructureManager.view(saved);
    }

    private SuggestionVO suggestionView(SuggestionDO suggestion) {
        try {
            return new SuggestionVO(suggestion.getId(), suggestion.getNodeId(), suggestion.getTreeRevision(),
                suggestion.getInstruction(),
                mapper.readValue(suggestion.getChildrenJson(), new TypeReference<List<CandidateDTO>>() {}),
                suggestion.getCreatedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid stored suggestionRepository", exception);
        }
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
