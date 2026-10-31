package com.aichat.repository;

import com.aichat.model.entity.LoginSessionDO;
import org.springframework.data.jpa.repository.JpaRepository;

/** LoginSession 的持久化访问接口，查询由 Spring Data JPA 实现。 */
public interface LoginSessionRepository extends JpaRepository<LoginSessionDO, String> {}
