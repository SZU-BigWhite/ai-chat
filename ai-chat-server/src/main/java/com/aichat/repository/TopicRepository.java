package com.aichat.repository;

import com.aichat.model.entity.TopicDO;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Topic 的持久化访问接口，查询由 Spring Data JPA 实现。 */
public interface TopicRepository extends JpaRepository<TopicDO, String> {
    List<TopicDO> findAllByOrderBySortOrderAsc();

    List<TopicDO> findByEnabledTrueOrderBySortOrderAsc();
}
