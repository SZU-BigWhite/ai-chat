package com.aichat.repository;

import com.aichat.model.entity.ExportSnapshotDO;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** ExportSnapshot 的持久化访问接口，查询由 Spring Data JPA 实现。 */
public interface ExportSnapshotRepository extends JpaRepository<ExportSnapshotDO, String> {
    List<ExportSnapshotDO> findByTreeIdOrderByCreatedAtDesc(String treeId);

    void deleteByTreeId(String treeId);
}
