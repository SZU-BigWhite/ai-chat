package com.aichat.manager;

import com.aichat.config.TopicProperties;
import com.aichat.model.dto.SeedTopicDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 读取并一次性校验全部初始化主题，避免部分导入后才发现格式错误。 */
@Component
public class TopicSeedLoader {
    private final ObjectMapper mapper;
    private final Validator validator;
    private final TopicProperties properties;

    public TopicSeedLoader(ObjectMapper mapper, Validator validator, TopicProperties properties) {
        this.mapper = mapper;
        this.validator = validator;
        this.properties = properties;
    }

    public List<SeedTopicDTO> load() throws IOException {
        if (!properties.getSeedEnabled()) {
            return List.of();
        }
        List<SeedTopicDTO> seeds;
        try (var input = properties.getSeedLocation().getInputStream()) {
            seeds = mapper.readerFor(new TypeReference<List<SeedTopicDTO>>() {})
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(input);
        }
        if (seeds == null) {
            throw new IllegalStateException("Topic seed resource must contain a JSON array");
        }
        Set<String> ids = new HashSet<>();
        for (SeedTopicDTO seed : seeds) {
            if (seed == null || !validator.validate(seed).isEmpty() || !ids.add(seed.id())) {
                throw new IllegalStateException("Topic seed resource contains invalid fields or duplicate IDs");
            }
        }
        return seeds;
    }
}
