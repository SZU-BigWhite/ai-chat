package com.aichat.manager;

import com.aichat.constant.BusinessConstants;
import com.aichat.exception.ApiException;
import com.aichat.model.dto.CandidateDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 纯 JSON 输出协议的边界校验，不修补模型输出或剥离 Markdown。 */
@Component
public class AiResponseParser {
    private final ObjectMapper mapper;

    public AiResponseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** 解析 JSON 响应，并校验节点结构、长度和同层重名。 */
    public List<CandidateDTO> parse(String raw, int max) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 返回空结果，请重试");
        }
        JsonNode json;
        try {
            json = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(raw);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 返回的内容不是有效 JSON，请重试", exception);
        }
        JsonNode children = json.path("children");
        if (!children.isArray() || children.size() > BusinessConstants.MAX_AI_CANDIDATES) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 返回的分类结构不符合要求，请重试");
        }
        if (children.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "AI 未找到合理的下一层分类，请调整拆分要求或保留当前颗粒度");
        }
        List<CandidateDTO> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (JsonNode item : children) {
            if (!item.path("name").isTextual() || !item.path("description").isTextual()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 节点缺少名称或范围说明，请重试");
            }
            String name = item.get("name").asText().trim();
            String description = item.get("description").asText().trim();
            if (name.isBlank() || name.length() > BusinessConstants.MAX_NODE_NAME_LENGTH || description.isBlank()
                || description.length() > BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 节点内容不完整或过长，请重试");
            }
            if (names.add(name.toLowerCase(Locale.ROOT)) && result.size() < max) {
                result.add(new CandidateDTO(name, description));
            }
        }
        return result;
    }
}
