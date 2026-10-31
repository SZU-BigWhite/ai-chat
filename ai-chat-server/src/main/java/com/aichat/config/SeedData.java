package com.aichat.config;

import com.aichat.manager.TopicSeedLoader;
import com.aichat.model.dto.SeedTopicDTO;
import com.aichat.model.entity.TopicDO;
import com.aichat.repository.TopicRepository;
import com.aichat.service.AuthService;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 幂等初始化资源文件中的预置主题与管理员账号。 */
@Component
public class SeedData implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeedData.class);
    private final TopicRepository topics;
    private final AuthService auth;
    private final EntityManager entityManager;
    private final TopicSeedLoader loader;
    private final AdminProperties properties;

    public SeedData(TopicRepository topics, AuthService auth, EntityManager entityManager, TopicSeedLoader loader,
        AdminProperties properties) {
        this.topics = topics;
        this.auth = auth;
        this.entityManager = entityManager;
        this.loader = loader;
        this.properties = properties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) throws IOException {
        var seeds = loader.load();
        var existingIds = topics.findAll().stream().map(TopicDO::getId).collect(Collectors.toSet());
        // 只补齐缺失的预置主题，保留管理员对现有主题的编辑和停用状态。
        int inserted = 0;
        for (SeedTopicDTO seed : seeds) {
            if (existingIds.contains(seed.id())) {
                continue;
            }
            TopicDO topic = new TopicDO();
            topic.setId(seed.id());
            topic.setName(seed.name());
            topic.setDescription(seed.description());
            topic.setIcon(seed.icon());
            topic.setColor(seed.color());
            topic.setSortOrder(seed.sortOrder());
            entityManager.persist(topic);
            inserted++;
        }
        auth.bootstrapAdmin(properties.getEmail(), properties.getPassword());
        LOGGER.info("Topic seed initialization prepared inserted={} existing={}", inserted, existingIds.size());
    }
}
