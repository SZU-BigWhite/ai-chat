package com.aichat.repository;

import com.aichat.model.entity.AiUsageDO;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/** AiUsage 的持久化访问接口，查询由 Spring Data JPA 实现。 */
public interface AiUsageRepository extends JpaRepository<AiUsageDO, Long> {
    long countByUserIdAndCreatedAtAfter(String userId, Instant after);
}
