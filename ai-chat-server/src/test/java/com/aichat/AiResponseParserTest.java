package com.aichat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aichat.exception.ApiException;
import com.aichat.manager.AiResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** 模型输出是不可信边界：只接受单个纯 JSON 对象及有效节点。 */
class AiResponseParserTest {
    private final AiResponseParser parser = new AiResponseParser(new ObjectMapper());
    private static final String VALID_JSON = "{\"children\":[{\"name\":\"基础知识\",\"description\":\"基本概念\"}]}";

    @Test
    void parsesPureJson() {
        assertThat(parser.parse(VALID_JSON, 3)).hasSize(1);
        assertThat(parser.parse(VALID_JSON, 3).get(0).name()).isEqualTo("基础知识");
    }

    @Test
    void rejectsMarkdownAndTrailingContent() {
        for (String response : new String[] {"```json\n" + VALID_JSON + "\n```", VALID_JSON + "说明",
            VALID_JSON + " {}"}) {
            assertThatThrownBy(() -> parser.parse(response, 3)).isInstanceOf(ApiException.class);
        }
    }

    @Test
    void rejectsMissingFieldsAndWrongTypes() {
        for (String response : new String[] {"null", "[]", "{\"children\":{}}", "{\"children\":[{\"name\":1}]}"}) {
            assertThatThrownBy(() -> parser.parse(response, 3)).isInstanceOf(ApiException.class);
        }
    }
}
