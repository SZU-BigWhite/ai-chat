package com.aichat.service;

import com.aichat.model.dto.CandidateDTO;
import com.aichat.model.dto.ExpandNodeDTO;
import com.aichat.model.dto.ExtractTopicDTO;
import com.aichat.model.vo.SuggestionVO;
import java.util.List;

/** AiService 的业务契约，接口协议与持久化实现分离。 */
public interface AiService {
    /** 查询模型调用配置是否完整。 */
    boolean configured();

    /** 从描述中提取候选主题。 */
    List<CandidateDTO> extract(String userId, ExtractTopicDTO request);

    /** 对指定节点生成下一层候选，等待用户确认。 */
    SuggestionVO expand(String userId, String treeId, String nodeId, ExpandNodeDTO request);
}
