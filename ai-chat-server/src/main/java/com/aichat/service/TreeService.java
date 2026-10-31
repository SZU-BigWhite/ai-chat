package com.aichat.service;

import com.aichat.model.dto.AcceptSuggestionDTO;
import com.aichat.model.dto.AddNodeDTO;
import com.aichat.model.dto.CandidateDTO;
import com.aichat.model.dto.CreateTreeDTO;
import com.aichat.model.dto.EditNodeDTO;
import com.aichat.model.entity.KnowledgeTreeDO;
import com.aichat.model.vo.ExportContentVO;
import com.aichat.model.vo.ExportVersionVO;
import com.aichat.model.vo.SuggestionVO;
import com.aichat.model.vo.TreeExportVO;
import com.aichat.model.vo.TreeSummaryVO;
import com.aichat.model.vo.TreeVO;
import java.util.List;

/** TreeService 的业务契约，接口协议与持久化实现分离。 */
public interface TreeService {
    /** 按树 ID 和用户 ID 查询，其他用户的资源视为不存在。 */
    KnowledgeTreeDO getOwnedTree(String ownerId, String id);

    /** 查询完整知识树。 */
    TreeVO getTree(String ownerId, String id);

    /** 查询用户知识树及节点汇总。 */
    List<TreeSummaryVO> listTrees(String ownerId);

    /** 创建根主题。 */
    TreeVO createTree(String ownerId, CreateTreeDTO request);

    /** 更新节点；范围变更后将后代标记为待复核。 */
    TreeVO updateNode(String ownerId, String treeId, String nodeId, EditNodeDTO request);

    /** 手动添加子节点。 */
    TreeVO addNode(String ownerId, String treeId, String nodeId, AddNodeDTO request);

    /** 删除非根节点及其子树。 */
    TreeVO deleteNode(String ownerId, String treeId, String nodeId, long revision);

    /** 删除整棵树、候选建议和历史导出。 */
    void deleteTree(String ownerId, String treeId, long revision);

    /** 复制结构并重新生成全部节点 ID。 */
    TreeVO copyTree(String ownerId, String treeId);

    /** 仅返回当前树版本的未采纳建议。 */
    List<SuggestionVO> listPendingSuggestions(String ownerId, String treeId);

    /** 复查树版本后保存 AI 建议。 */
    SuggestionVO saveSuggestion(String ownerId, String treeId, String nodeId, long revision, String instruction,
        List<CandidateDTO> children);

    /** 在同一事务中采纳建议并更新树版本。 */
    TreeVO acceptSuggestion(String ownerId, String treeId, String suggestionId, AcceptSuggestionDTO request);

    /** 生成不含内部编辑状态的 JSON 视图。 */
    TreeExportVO getExportPreview(String ownerId, String treeId);

    /** 保存不可变导出快照。 */
    ExportContentVO createExport(String ownerId, String treeId, long revision);

    /** 查询历史导出的元信息。 */
    List<ExportVersionVO> listExportVersions(String ownerId, String treeId);

    /** 读取属于当前用户知识树的历史快照。 */
    ExportContentVO getExportVersion(String ownerId, String treeId, String versionId);
}
