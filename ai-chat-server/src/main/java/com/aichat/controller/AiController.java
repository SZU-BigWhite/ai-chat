package com.aichat.controller;

import com.aichat.constant.BusinessConstants;
import com.aichat.model.dto.CandidateDTO;
import com.aichat.model.dto.ExpandNodeDTO;
import com.aichat.model.dto.ExtractTopicDTO;
import com.aichat.model.vo.AiStatusVO;
import com.aichat.model.vo.SuggestionVO;
import com.aichat.model.vo.UserVO;
import com.aichat.service.AiService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供 AI 配置状态、主题提取和单层节点拆分接口。 */
@RestController
@RequestMapping("/api")
public class AiController {
    private final AiService ai;

    public AiController(AiService ai) {
        this.ai = ai;
    }

    @GetMapping("/ai/status")
    public AiStatusVO status() {
        return new AiStatusVO(ai.configured());
    }

    @PostMapping("/ai/extract")
    public List<CandidateDTO> extract(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @Valid @RequestBody ExtractTopicDTO request) {
        return ai.extract(user.id(), request);
    }

    @PostMapping("/trees/{id}/nodes/{nodeId}/expand")
    public SuggestionVO expand(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id, @PathVariable String nodeId, @Valid @RequestBody ExpandNodeDTO request) {
        return ai.expand(user.id(), id, nodeId, request);
    }
}
