package com.aichat.converter;

import com.aichat.model.entity.TopicDO;
import com.aichat.model.vo.TopicVO;

/** 显式控制主题接口的对外字段。 */
public final class TopicConverter {
    private TopicConverter() {}

    public static TopicVO toView(TopicDO topic) {
        return new TopicVO(topic.getId(), topic.getName(), topic.getDescription(), topic.getIcon(), topic.getColor(),
            topic.getSortOrder(), topic.getEnabled());
    }
}
