package com.aichat.converter;

import com.aichat.model.entity.UserAccountDO;
import com.aichat.model.vo.UserVO;

/** 账号持久化模型到公开视图的单向转换。 */
public final class AccountConverter {
    private AccountConverter() {}

    public static UserVO toView(UserAccountDO user) {
        return new UserVO(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getEnabled(),
            user.getCreatedAt());
    }
}
