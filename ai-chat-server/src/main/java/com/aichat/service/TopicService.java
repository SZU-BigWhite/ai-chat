package com.aichat.service;

import com.aichat.model.vo.TopicVO;
import java.util.List;

/** 面向普通用户的主题查询。 */
public interface TopicService {
    /** 查询当前启用的预置主题，按管理端顺序排列。 */
    List<TopicVO> listEnabledTopics();
}
