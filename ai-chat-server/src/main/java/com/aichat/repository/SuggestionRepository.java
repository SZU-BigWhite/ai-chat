package com.aichat.repository;

import com.aichat.model.entity.SuggestionDO;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Suggestion 的持久化访问接口，查询由 Spring Data JPA 实现。 */
public interface SuggestionRepository extends JpaRepository<SuggestionDO, String> {
    List<SuggestionDO> findByTreeIdAndAcceptedFalseOrderByCreatedAtDesc(String treeId);

    void deleteByTreeId(String treeId);
}
