package com.aichat.service;

import com.aichat.model.dto.LoginDTO;
import com.aichat.model.dto.RegisterDTO;
import com.aichat.model.vo.AuthVO;
import com.aichat.model.vo.UserVO;

/** AuthService 的业务契约，接口协议与持久化实现分离。 */
public interface AuthService {
    /** 注册账号并签发会话。 */
    AuthVO register(RegisterDTO request);

    /** 验证凭证并签发会话。 */
    AuthVO login(LoginDTO request);

    /** 校验令牌并返回当前有效账号。 */
    UserVO authenticate(String header);

    /** 撤销指定会话。 */
    void logout(String header);

    /** 仅在账号不存在时初始化管理员。 */
    void bootstrapAdmin(String email, String password);
}
