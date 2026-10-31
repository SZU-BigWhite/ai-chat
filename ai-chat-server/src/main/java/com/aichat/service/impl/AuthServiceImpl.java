package com.aichat.service.impl;

import com.aichat.config.AdminProperties;
import com.aichat.config.AuthProperties;
import com.aichat.constant.BusinessConstants;
import com.aichat.converter.AccountConverter;
import com.aichat.exception.ApiException;
import com.aichat.model.dto.LoginDTO;
import com.aichat.model.dto.RegisterDTO;
import com.aichat.model.entity.LoginSessionDO;
import com.aichat.model.entity.UserAccountDO;
import com.aichat.model.enums.UserRoleEnum;
import com.aichat.model.vo.AuthVO;
import com.aichat.model.vo.UserVO;
import com.aichat.repository.LoginSessionRepository;
import com.aichat.repository.UserAccountRepository;
import com.aichat.service.AuthService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 负责账号注册、密码校验与服务端会话管理。 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserAccountRepository userRepository;
    private final LoginSessionRepository sessionRepository;
    private final AuthProperties properties;
    private final AdminProperties adminProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder =
        new BCryptPasswordEncoder(BusinessConstants.PASSWORD_HASH_STRENGTH);
    // 未知邮箱也做一次 BCrypt 比较，减少通过响应时间枚举账号的差异。
    private final String dummyPasswordHash = passwordEncoder.encode("timing-only-placeholder");

    public AuthServiceImpl(UserAccountRepository userRepository, LoginSessionRepository sessionRepository,
        AuthProperties properties, AdminProperties adminProperties, Clock clock) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.properties = properties;
        this.adminProperties = adminProperties;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AuthVO register(RegisterDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "此邮箱已注册");
        }
        checkPassword(request.password());
        UserAccountDO user = new UserAccountDO();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.saveAndFlush(user);
        LOGGER.info("Account registration persisted userId={}", user.getId());
        return issue(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AuthVO login(LoginDTO request) {
        UserAccountDO user = userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BusinessConstants.MAX_PASSWORD_BYTES) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "邮箱或密码不正确");
        }
        boolean valid =
            passwordEncoder.matches(request.password(), user == null ? dummyPasswordHash : user.getPasswordHash());
        if (user == null || !valid || !user.getEnabled()) {
            LOGGER.warn("LoginDTO rejected reason=invalid_credentials_or_disabled");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "邮箱或密码不正确，或账号已停用");
        }
        LOGGER.info("LoginDTO verified userId={}", user.getId());
        return issue(user);
    }

    /** 原始令牌只返回给客户端；数据库仅保存摘要，按配置的有效期失效。 */
    private AuthVO issue(UserAccountDO user) {
        byte[] bytes = new byte[BusinessConstants.SESSION_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LoginSessionDO session = new LoginSessionDO();
        session.setTokenHash(hash(token));
        session.setUserId(user.getId());
        session.setExpiresAt(clock.instant().plus(properties.getSessionTtl()));
        sessionRepository.save(session);
        return new AuthVO(token, AccountConverter.toView(user));
    }

    @Override
    public UserVO authenticate(String header) {
        if (header == null || !header.startsWith(BusinessConstants.BEARER_PREFIX)
            || header.length() > BusinessConstants.MAX_AUTH_HEADER_LENGTH) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        LoginSessionDO session =
            sessionRepository.findById(hash(header.substring(BusinessConstants.BEARER_PREFIX.length())))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录"));
        if (!session.getExpiresAt().isAfter(clock.instant())) {
            sessionRepository.delete(session);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "登录已过期");
        }
        UserAccountDO user = userRepository.findById(session.getUserId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "账号不存在"));
        if (!user.getEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号已停用");
        }
        return AccountConverter.toView(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void logout(String header) {
        if (header != null && header.startsWith(BusinessConstants.BEARER_PREFIX)) {
            sessionRepository.deleteById(hash(header.substring(BusinessConstants.BEARER_PREFIX.length())));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void bootstrapAdmin(String email, String password) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).isPresent()) {
            return;
        }
        if (password.length() < BusinessConstants.MIN_ADMIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                "ADMIN_PASSWORD must contain at least " + BusinessConstants.MIN_ADMIN_PASSWORD_LENGTH + " characters");
        }
        checkPassword(password);
        UserAccountDO user = new UserAccountDO();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setName(adminProperties.getDisplayName());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRoleEnum.ADMIN.name());
        userRepository.save(user);
        LOGGER.info("Bootstrap administrator persisted userId={}", user.getId());
    }

    private void checkPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BusinessConstants.MAX_PASSWORD_BYTES) {
            throw ApiException.bad("密码 UTF-8 长度不能超过 " + BusinessConstants.MAX_PASSWORD_BYTES + " 字节");
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
