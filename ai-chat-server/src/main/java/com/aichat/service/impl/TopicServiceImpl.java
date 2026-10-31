package com.aichat.service.impl;

import com.aichat.converter.TopicConverter;
import com.aichat.model.vo.TopicVO;
import com.aichat.repository.TopicRepository;
import com.aichat.service.TopicService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 主题查询的数据库实现。 */
@Service
@Transactional(readOnly = true)
public class TopicServiceImpl implements TopicService {
    private final TopicRepository topicRepository;

    public TopicServiceImpl(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Override
    public List<TopicVO> listEnabledTopics() {
        return topicRepository.findByEnabledTrueOrderBySortOrderAsc().stream().map(TopicConverter::toView).toList();
    }
}
