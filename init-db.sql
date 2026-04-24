CREATE TABLE IF NOT EXISTS user_session (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(255) NOT NULL,
    token_hash      VARCHAR(64)  NOT NULL UNIQUE,
    rotate_at       TIMESTAMP    NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    invalidated     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_token_hash ON user_session (token_hash);
CREATE INDEX IF NOT EXISTS idx_username ON user_session (username);
