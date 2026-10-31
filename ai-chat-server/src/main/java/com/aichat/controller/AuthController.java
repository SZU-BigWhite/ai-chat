package com.aichat.controller;

import com.aichat.config.AppReadiness;
import com.aichat.constant.BusinessConstants;
import com.aichat.model.dto.LoginDTO;
import com.aichat.model.dto.RegisterDTO;
import com.aichat.model.vo.AuthVO;
import com.aichat.model.vo.HealthVO;
import com.aichat.model.vo.SuccessVO;
import com.aichat.model.vo.UserVO;
import com.aichat.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供登录、注册、会话查询与服务就绪检查。 */
@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;
    private final AppReadiness readiness;
    private final String applicationName;

    public AuthController(AuthService auth, AppReadiness readiness,
        @Value("${spring.application.name}") String applicationName) {
        this.auth = auth;
        this.readiness = readiness;
        this.applicationName = applicationName;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthVO> health() {
        boolean ready = readiness.isReady();
        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
            .body(new HealthVO(ready ? "ok" : "starting", applicationName));
    }

    @PostMapping("/auth/register")
    public AuthVO register(@Valid @RequestBody RegisterDTO request) {
        return auth.register(request);
    }

    @PostMapping("/auth/login")
    public AuthVO login(@Valid @RequestBody LoginDTO request) {
        return auth.login(request);
    }

    @PostMapping("/auth/logout")
    public SuccessVO logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        auth.logout(token);
        return new SuccessVO(true);
    }

    @GetMapping("/auth/me")
    public UserVO me(@RequestAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE) UserVO user) {
        return user;
    }
}
