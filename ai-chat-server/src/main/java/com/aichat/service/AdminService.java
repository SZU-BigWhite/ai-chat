package com.aichat.service;

import com.aichat.model.dto.UpdateTopicDTO;
import com.aichat.model.dto.UpdateUserDTO;
import com.aichat.model.vo.AdminStatsVO;
import com.aichat.model.vo.TopicVO;
import com.aichat.model.vo.UserVO;
import java.util.List;

/** 管理员可执行的主题、账号和用量管理操作。 */
public interface AdminService {
    /** 查询所有主题，包含停用主题。 */
    List<TopicVO> listTopics();

    /** 创建预置主题。 */
    TopicVO saveTopic(UpdateTopicDTO request);

    /** 更新预置主题。 */
    TopicVO updateTopic(String topicId, UpdateTopicDTO request);

    /** 查询公开账号信息。 */
    List<UserVO> listUsers();

    /** 更新用户启用状态，禁止停用管理员。 */
    UserVO updateUser(String userId, UpdateUserDTO request);

    /** 获取汇总计数及最近一百条调用记录。 */
    AdminStatsVO getStats();
}
