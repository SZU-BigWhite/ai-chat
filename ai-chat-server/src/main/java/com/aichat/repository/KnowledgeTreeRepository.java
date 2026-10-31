package com.aichat.repository;

import com.aichat.model.entity.KnowledgeTreeDO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** KnowledgeTree 的持久化访问接口，查询由 Spring Data JPA 实现。 */
public interface KnowledgeTreeRepository extends JpaRepository<KnowledgeTreeDO, String> {
    List<KnowledgeTreeDO> findByOwnerIdOrderByUpdatedAtDesc(String ownerId);

    Optional<KnowledgeTreeDO> findByIdAndOwnerId(String id, String ownerId);
}
