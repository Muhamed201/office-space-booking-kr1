-- Таблица refresh-сессий
CREATE TABLE IF NOT EXISTS refresh_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id),
    refresh_token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_session_token ON refresh_session(refresh_token);
CREATE INDEX idx_refresh_session_user ON refresh_session(user_id);
