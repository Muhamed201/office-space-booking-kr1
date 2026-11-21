CREATE TABLE user_role (name VARCHAR(20) PRIMARY KEY);
INSERT INTO user_role(name) VALUES ('EMPLOYEE'), ('OFFICE_MANAGER'), ('ADMIN');
ALTER TABLE user_account
    ADD CONSTRAINT user_account_role_fk FOREIGN KEY (role) REFERENCES user_role(name),
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_audit_created_at ON audit_event(created_at);
