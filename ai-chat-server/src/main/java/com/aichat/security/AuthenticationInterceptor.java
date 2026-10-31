package com.aichat.security;

import com.aichat.constant.BusinessConstants;
import com.aichat.controller.AdminController;
import com.aichat.exception.ApiException;
import com.aichat.model.enums.UserRoleEnum;
import com.aichat.model.vo.UserVO;
import com.aichat.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 认证、登录限流与管理员授权入口。 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    private final AuthService authService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public AuthenticationInterceptor(AuthService authService, LoginAttemptLimiter loginAttemptLimiter) {
        this.authService = authService;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("/api/health".equals(path)) {
            return true;
        }
        if ("/api/auth/login".equals(path) || "/api/auth/register".equals(path)) {
            loginAttemptLimiter.check(request.getRemoteAddr());
            return true;
        }
        UserVO user = authService.authenticate(request.getHeader(HttpHeaders.AUTHORIZATION));
        // 使用实际解析出的控制器校验权限，防止 URL 编码与矩阵参数绕过。
        if (handler instanceof HandlerMethod method && AdminController.class.isAssignableFrom(method.getBeanType())
            && !UserRoleEnum.ADMIN.name().equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        request.setAttribute(BusinessConstants.CURRENT_USER_ATTRIBUTE, user);
        return true;
    }
}
