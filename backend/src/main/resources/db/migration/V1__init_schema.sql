-- =====================================================
-- Flyway Migration V1: 初始化 schema
-- 创建 users 表和 public_stories 表
-- =====================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          PRIMARY KEY,
    username        VARCHAR(20)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  users              IS '用户表';
COMMENT ON COLUMN users.id           IS '用户ID（雪花算法）';
COMMENT ON COLUMN users.username     IS '用户名，3-20字符，唯一';
COMMENT ON COLUMN users.password_hash IS '密码哈希 (BCrypt)';
COMMENT ON COLUMN users.created_at   IS '注册时间';
COMMENT ON COLUMN users.updated_at   IS '最后更新时间';
COMMENT ON COLUMN users.deleted      IS '逻辑删除: 0-正常, 1-已删除';

CREATE INDEX idx_users_username ON users(username) WHERE deleted = 0;

-- 公开故事表
CREATE TABLE IF NOT EXISTS public_stories (
    id              BIGINT          PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    fragment_count  INT             NOT NULL DEFAULT 0,
    length          VARCHAR(10)     NOT NULL,
    style           VARCHAR(20)     NOT NULL,
    view_count      INT             NOT NULL DEFAULT 0,
    report_count    INT             NOT NULL DEFAULT 0,
    is_visible      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  public_stories               IS '公开故事表（故事广场）';
COMMENT ON COLUMN public_stories.user_id        IS '作者用户ID';
COMMENT ON COLUMN public_stories.fragment_count IS '使用的碎片数量';
COMMENT ON COLUMN public_stories.length         IS '故事长度: short/medium/long';
COMMENT ON COLUMN public_stories.style          IS '故事风格: scifi/fantasy/realistic/prose/poetry/mystery';
COMMENT ON COLUMN public_stories.is_visible     IS '是否可见（下架=不可见，但保留数据）';

CREATE INDEX idx_public_stories_user_id    ON public_stories(user_id, deleted);
CREATE INDEX idx_public_stories_created_at ON public_stories(created_at DESC) WHERE is_visible = TRUE AND deleted = 0;

-- 更新时间触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_public_stories_updated_at
    BEFORE UPDATE ON public_stories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
