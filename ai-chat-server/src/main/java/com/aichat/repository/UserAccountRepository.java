package com.aichat.repository;

import com.aichat.model.entity.UserAccountDO;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** UserAccount 的持久化访问接口，查询由 Spring Data JPA 实现。 */
public interface UserAccountRepository extends JpaRepository<UserAccountDO, String> {
    Optional<UserAccountDO> findByEmail(String email);
}
