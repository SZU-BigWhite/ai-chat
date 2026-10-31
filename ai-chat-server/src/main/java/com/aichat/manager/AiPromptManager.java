package com.aichat.manager;

import com.aichat.config.AiPromptProperties;
import com.aichat.constant.BusinessConstants;
import com.aichat.model.enums.AiOperationEnum;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** 启动时加载提示词，只替换明确的规则占位符，保留 JSON 示例中的大括号。 */
@Component
public class AiPromptManager {
    private final String extract;
    private final String expand;

    public AiPromptManager(AiPromptProperties properties) throws IOException {
        extract = load(properties.getExtract());
        expand = load(properties.getExpand());
    }

    public String render(AiOperationEnum operation, int maxCandidates) {
        String template = switch (operation) {
            case EXTRACT -> extract;
            case EXPAND -> expand;
        };
        return template.replace("{{maxCandidates}}", Integer.toString(maxCandidates))
            .replace("{{maxNameLength}}", Integer.toString(BusinessConstants.MAX_NODE_NAME_LENGTH))
            .replace("{{maxDescriptionLength}}", Integer.toString(BusinessConstants.MAX_NODE_DESCRIPTION_LENGTH));
    }

    private String load(Resource resource) throws IOException {
        String template = resource.getContentAsString(StandardCharsets.UTF_8);
        if (template.isBlank()) {
            throw new IllegalStateException("AI prompt resource must not be blank");
        }
        return template;
    }
}
