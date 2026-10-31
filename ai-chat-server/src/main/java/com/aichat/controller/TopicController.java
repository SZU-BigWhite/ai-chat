package com.aichat.controller;

import com.aichat.model.vo.TopicVO;
import com.aichat.service.TopicService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 向已登录用户提供当前启用的预置主题。 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {
    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<TopicVO> listTopics() {
        return topicService.listEnabledTopics();
    }
}
