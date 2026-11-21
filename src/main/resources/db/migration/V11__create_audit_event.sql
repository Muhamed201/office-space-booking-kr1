-- Таблица аудита
CREATE TABLE IF NOT EXISTS audit_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES user_account(id),
    event_type VARCHAR(20) NOT NULL CHECK (event_type IN ('LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'ARCHIVE', 'RESTORE', 'DELETE', 'LOGIN_FAILED')),
    entity_type VARCHAR(50),
    entity_id UUID,
    description VARCHAR(500),
    correlation_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_event_type ON audit_event(event_type);
CREATE INDEX idx_audit_event_user ON audit_event(user_id);
CREATE INDEX idx_audit_event_created ON audit_event(created_at);
