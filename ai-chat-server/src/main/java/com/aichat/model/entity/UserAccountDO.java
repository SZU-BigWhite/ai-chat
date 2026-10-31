package com.aichat.model.entity;

import com.aichat.constant.BusinessConstants;
import com.aichat.model.enums.UserRoleEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 用户账号与权限，密码仅存储 BCrypt 哈希。 */
@Entity
@Table(name = "user_account")
public class UserAccountDO {
    @Id
    private String id;

    @Column(nullable = false, unique = true, length = BusinessConstants.MAX_EMAIL_LENGTH)
    private String email;

    @Column(nullable = false, length = BusinessConstants.MAX_USER_NAME_LENGTH)
    private String name;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = UserRoleEnum.USER.name();

    private Boolean enabled = true;
    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
