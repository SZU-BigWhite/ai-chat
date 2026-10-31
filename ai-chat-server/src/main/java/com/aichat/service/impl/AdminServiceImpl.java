package com.aichat.service.impl;

import com.aichat.config.AdminProperties;
import com.aichat.config.TopicProperties;
import com.aichat.converter.AccountConverter;
import com.aichat.converter.TopicConverter;
import com.aichat.exception.ApiException;
import com.aichat.model.dto.UpdateTopicDTO;
import com.aichat.model.dto.UpdateUserDTO;
import com.aichat.model.entity.AiUsageDO;
import com.aichat.model.entity.TopicDO;
import com.aichat.model.entity.UserAccountDO;
import com.aichat.model.enums.UserRoleEnum;
import com.aichat.model.vo.AdminStatsVO;
import com.aichat.model.vo.AiUsageVO;
import com.aichat.model.vo.TopicVO;
import com.aichat.model.vo.UserVO;
import com.aichat.repository.AiUsageRepository;
import com.aichat.repository.KnowledgeTreeRepository;
import com.aichat.repository.TopicRepository;
import com.aichat.repository.UserAccountRepository;
import com.aichat.service.AdminService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端业务逻辑；控制器不直接访问持久化层。 */
@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);
    private final AdminProperties properties;
    private final TopicProperties topicProperties;
    private final TopicRepository topicRepository;
    private final UserAccountRepository userRepository;
    private final AiUsageRepository usageRepository;
    private final KnowledgeTreeRepository treeRepository;

    public AdminServiceImpl(TopicRepository topicRepository, UserAccountRepository userRepository,
        AiUsageRepository usageRepository, KnowledgeTreeRepository treeRepository, AdminProperties properties,
        TopicProperties topicProperties) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.usageRepository = usageRepository;
        this.treeRepository = treeRepository;
        this.properties = properties;
        this.topicProperties = topicProperties;
    }

    @Override
    public List<TopicVO> listTopics() {
        return topicRepository.findAllByOrderBySortOrderAsc().stream().map(TopicConverter::toView).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicVO saveTopic(UpdateTopicDTO request) {
        TopicDO topic = new TopicDO();
        topic.setId(UUID.randomUUID().toString());
        return persistTopic(topic, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicVO updateTopic(String topicId, UpdateTopicDTO request) {
        return persistTopic(topicRepository.findById(topicId).orElseThrow(ApiException::missing), request);
    }

    private TopicVO persistTopic(TopicDO topic, UpdateTopicDTO request) {
        topic.setName(request.name().trim());
        topic.setDescription(request.description().trim());
        topic.setIcon(request.icon());
        topic.setColor(request.color() == null ? topicProperties.getDefaultColor() : request.color());
        topic.setSortOrder(request.sortOrder());
        topic.setEnabled(request.enabled());
        TopicDO saved = topicRepository.save(topic);
        LOGGER.info("Topic update scheduled topicId={} enabled={}", saved.getId(), saved.getEnabled());
        return TopicConverter.toView(saved);
    }

    @Override
    public List<UserVO> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(AccountConverter::toView)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(String userId, UpdateUserDTO request) {
        UserAccountDO user = userRepository.findById(userId).orElseThrow(ApiException::missing);
        if (UserRoleEnum.ADMIN.name().equals(user.getRole()) && !request.enabled()) {
            throw ApiException.bad("不能停用管理员账号");
        }
        user.setEnabled(request.enabled());
        LOGGER.info("Account status update scheduled userId={} enabled={}", userId, request.enabled());
        return AccountConverter.toView(userRepository.save(user));
    }

    @Override
    public AdminStatsVO getStats() {
        List<AiUsageVO> recent = usageRepository
            .findAll(PageRequest.of(0, properties.getRecentUsageLimit(), Sort.by(Sort.Direction.DESC, "createdAt")))
            .stream().map(this::toUsageView).toList();
        return new AdminStatsVO(userRepository.count(), treeRepository.count(), usageRepository.count(), recent);
    }

    private AiUsageVO toUsageView(AiUsageDO usage) {
        return new AiUsageVO(usage.getId(), usage.getUserId(), usage.getOperation(), usage.getModel(),
            usage.getSuccess(), usage.getTotalTokens(), usage.getDurationMs(), usage.getCreatedAt());
    }
}
