package com.aichat.controller;

import com.aichat.constant.BusinessConstants;
import com.aichat.model.dto.AcceptSuggestionDTO;
import com.aichat.model.dto.AddNodeDTO;
import com.aichat.model.dto.CreateTreeDTO;
import com.aichat.model.dto.EditNodeDTO;
import com.aichat.model.vo.ExportVersionVO;
import com.aichat.model.vo.SuccessVO;
import com.aichat.model.vo.SuggestionVO;
import com.aichat.model.vo.TreeExportVO;
import com.aichat.model.vo.TreeSummaryVO;
import com.aichat.model.vo.TreeVO;
import com.aichat.model.vo.UserVO;
import com.aichat.service.TreeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 知识树接口入口；身份由拦截器解析，所有权和版本由业务层校验。 */
@RestController
@RequestMapping("/api/trees")
public class TreeController {
    private final TreeService trees;

    public TreeController(TreeService trees) {
        this.trees = trees;
    }

    @GetMapping
    public List<TreeSummaryVO> list(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user) {
        return trees.listTrees(user.id());
    }

    @PostMapping
    public TreeVO create(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @Valid @RequestBody CreateTreeDTO request) {
        return trees.createTree(user.id(), request);
    }

    @GetMapping("/{id}")
    public TreeVO detail(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id) {
        return trees.getTree(user.id(), id);
    }

    @PostMapping("/{id}/copy")
    public TreeVO copy(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id) {
        return trees.copyTree(user.id(), id);
    }

    @DeleteMapping("/{id}")
    public SuccessVO delete(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id, @RequestParam long revision) {
        trees.deleteTree(user.id(), id, revision);
        return new SuccessVO(true);
    }

    @PatchMapping("/{id}/nodes/{nodeId}")
    public TreeVO edit(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user, @PathVariable String id,
        @PathVariable String nodeId, @Valid @RequestBody EditNodeDTO request) {
        return trees.updateNode(user.id(), id, nodeId, request);
    }

    @PostMapping("/{id}/nodes/{nodeId}/children")
    public TreeVO add(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user, @PathVariable String id,
        @PathVariable String nodeId, @Valid @RequestBody AddNodeDTO request) {
        return trees.addNode(user.id(), id, nodeId, request);
    }

    @DeleteMapping("/{id}/nodes/{nodeId}")
    public TreeVO deleteNode(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id, @PathVariable String nodeId, @RequestParam long revision) {
        return trees.deleteNode(user.id(), id, nodeId, revision);
    }

    @GetMapping("/{id}/suggestions")
    public List<SuggestionVO> pending(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id) {
        return trees.listPendingSuggestions(user.id(), id);
    }

    @PostMapping("/{id}/suggestions/{suggestionId}/accept")
    public TreeVO accept(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id, @PathVariable String suggestionId, @Valid @RequestBody AcceptSuggestionDTO request) {
        return trees.acceptSuggestion(user.id(), id, suggestionId, request);
    }

    @GetMapping("/{id}/export-preview")
    public TreeExportVO preview(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id) {
        return trees.getExportPreview(user.id(), id);
    }

    @PostMapping("/{id}/exports")
    public ResponseEntity<String> export(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id, @RequestParam long revision) {
        return download(trees.createExport(user.id(), id, revision).content(), id);
    }

    @GetMapping("/{id}/exports")
    public List<ExportVersionVO> versions(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id) {
        return trees.listExportVersions(user.id(), id);
    }

    @GetMapping("/{id}/exports/{versionId}")
    public ResponseEntity<String> version(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user,
        @PathVariable String id, @PathVariable String versionId) {
        return download(trees.getExportVersion(user.id(), id, versionId).content(), id);
    }

    private ResponseEntity<String> download(String content, String id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=topic-" + id + ".json").body(content);
    }
}
