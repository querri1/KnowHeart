-- KnowHeart 数据库初始化脚本（在 MySQL / IDEA 中执行）
CREATE DATABASE IF NOT EXISTS knowheart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE knowheart;

-- 用户画像表（与 UserProfile 实体 @Table(name = "user_profile") 一致）
CREATE TABLE IF NOT EXISTS user_profile (
    user_id       VARCHAR(100)  NOT NULL COMMENT '用户 ID（主键）',
    name          VARCHAR(50)   NULL COMMENT '姓名',
    gender        VARCHAR(10)   NULL COMMENT '性别',
    age           INT           NULL COMMENT '年龄',
    city          VARCHAR(50)   NULL COMMENT '城市',
    relationship  VARCHAR(20)   NULL COMMENT '恋爱状态',
    interests     VARCHAR(2000) NULL COMMENT '兴趣爱好（JSON）',
    preferences   VARCHAR(2000) NULL COMMENT '约会偏好（JSON）',
    created_at    DATETIME      NULL COMMENT '创建时间',
    updated_at    DATETIME      NULL COMMENT '更新时间',
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像';

-- 账号表（登录注册）
CREATE TABLE IF NOT EXISTS app_user (
    user_id       VARCHAR(100)  NOT NULL COMMENT '用户 ID（主键，与 user_profile 关联）',
    username      VARCHAR(50)   NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(100)  NOT NULL COMMENT '密码哈希',
    nickname      VARCHAR(50)   NULL COMMENT '昵称',
    auth_token    VARCHAR(100)  NULL COMMENT '登录令牌',
    token_expiry  DATETIME      NULL COMMENT '令牌过期时间',
    created_at    DATETIME      NULL COMMENT '注册时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_auth_token (auth_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号';

-- 对话会话（每用户最多 10 条，由应用层控制）
CREATE TABLE IF NOT EXISTS chat_conversation (
    conversation_id VARCHAR(100) NOT NULL COMMENT '会话 ID',
    user_id         VARCHAR(100) NOT NULL COMMENT '所属用户',
    title           VARCHAR(100) NULL COMMENT '会话标题',
    created_at      DATETIME     NULL COMMENT '创建时间',
    updated_at      DATETIME     NULL COMMENT '最后更新时间',
    PRIMARY KEY (conversation_id),
    KEY idx_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话';

-- 对话消息
CREATE TABLE IF NOT EXISTS chat_message (
    message_id      VARCHAR(100) NOT NULL COMMENT '消息 ID',
    conversation_id VARCHAR(100) NOT NULL COMMENT '所属会话',
    role            VARCHAR(20)  NOT NULL COMMENT 'USER / ASSISTANT',
    content         TEXT         NULL COMMENT '消息内容',
    created_at      DATETIME     NULL COMMENT '发送时间',
    PRIMARY KEY (message_id),
    KEY idx_conversation_time (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息';
