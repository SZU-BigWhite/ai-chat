package com.aichat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aichat.config.AiPromptProperties;
import com.aichat.config.TopicProperties;
import com.aichat.manager.AiPromptManager;
import com.aichat.manager.TopicSeedLoader;
import com.aichat.model.enums.AiOperationEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;

/** 资源覆盖及损坏文件校验，保证初始化数据和提示规则可靠加载。 */
class ExternalResourceTest {
    @TempDir
    Path directory;

    @Test
    void bundledSeedsRemainCompatibleAndDisabledSeedingDoesNotReadMissingFile() throws Exception {
        TopicProperties properties = new TopicProperties();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            TopicSeedLoader loader = new TopicSeedLoader(new ObjectMapper(), factory.getValidator(), properties);
            assertThat(loader.load()).hasSize(12).anySatisfy(seed -> {
                assertThat(seed.id()).isEqualTo("health");
                assertThat(seed.sortOrder()).isZero();
            });
            properties.setSeedLocation(new FileSystemResource(directory.resolve("missing.json")));
            properties.setSeedEnabled(false);
            assertThat(loader.load()).isEmpty();
        }
    }

    @Test
    void externalSeedsRejectDuplicatesInvalidFieldsAndTrailingContent() throws Exception {
        Path file = directory.resolve("topics.json");
        TopicProperties properties = new TopicProperties();
        properties.setSeedLocation(new FileSystemResource(file));
        String topic =
            "{\"id\":\"custom\",\"name\":\"自定义\",\"description\":\"范围\",\"icon\":\"Atom\",\"color\":\"#123456\",\"sortOrder\":2}";
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            TopicSeedLoader loader = new TopicSeedLoader(new ObjectMapper(), factory.getValidator(), properties);
            Files.writeString(file, "[" + topic + "]");
            assertThat(loader.load().get(0).name()).isEqualTo("自定义");
            for (String invalid : new String[] {"[" + topic + "," + topic + "]",
                "[" + topic.replace("#123456", "red") + "]", "null", "[null]", "[" + topic + "] {}"}) {
                Files.writeString(file, invalid);
                assertThatThrownBy(loader::load).isInstanceOf(Exception.class);
            }
        }
    }

    @Test
    void promptsRenderActualLimitsAndSupportExternalOverrides() throws Exception {
        AiPromptProperties properties = new AiPromptProperties();
        AiPromptManager defaults = new AiPromptManager(properties);
        assertThat(defaults.render(AiOperationEnum.EXTRACT, 5)).contains("1 至 5", "100", "2000").doesNotContain("{{");
        assertThat(defaults.render(AiOperationEnum.EXPAND, 7)).contains("最多 7 个").doesNotContain("{{");
        Path file = directory.resolve("extract.txt");
        Files.writeString(file, "候选最多 {{maxCandidates}}；协议示例 {\"children\":[]}");
        properties.setExtract(new FileSystemResource(file));
        assertThat(new AiPromptManager(properties).render(AiOperationEnum.EXTRACT, 4))
            .isEqualTo("候选最多 4；协议示例 {\"children\":[]}");
        Files.writeString(file, " ");
        assertThatThrownBy(() -> new AiPromptManager(properties)).isInstanceOf(IllegalStateException.class);
    }
}
