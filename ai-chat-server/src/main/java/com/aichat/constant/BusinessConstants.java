package com.aichat.constant;

/** API、存储与密码算法的固定边界；可调整的运行策略由配置类管理。 */
public final class BusinessConstants {
    public static final int MAX_SUPPORTED_TREE_DEPTH = 80;
    public static final int MAX_NODE_NAME_LENGTH = 100;
    public static final int MAX_NODE_DESCRIPTION_LENGTH = 2000;
    public static final int MAX_TOPIC_INPUT_LENGTH = 3000;
    public static final int MAX_EMAIL_LENGTH = 160;
    public static final int MAX_USER_NAME_LENGTH = 60;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 64;
    public static final int MAX_TOPIC_ICON_LENGTH = 40;
    public static final int MIN_AI_CANDIDATES = 2;
    public static final String HEX_COLOR_PATTERN = "#[0-9a-fA-F]{6}";
    public static final int MAX_AI_CANDIDATES = 10;
    public static final int MAX_PASSWORD_BYTES = 72;
    public static final int MIN_ADMIN_PASSWORD_LENGTH = 12;
    public static final int PASSWORD_HASH_STRENGTH = 12;
    public static final int SESSION_TOKEN_BYTES = 32;
    public static final int MAX_AUTH_HEADER_LENGTH = 150;
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";
    public static final String EXPORT_SCHEMA_VERSION = "1.0";

    private BusinessConstants() {}
}
