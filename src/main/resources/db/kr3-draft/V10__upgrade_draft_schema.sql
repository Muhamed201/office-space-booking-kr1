ALTER TABLE booking ADD COLUMN description VARCHAR(2000),
                    DROP CONSTRAINT booking_workspace_no_overlap,
                    DROP CONSTRAINT booking_employee_no_overlap;
ALTER TABLE booking
    ADD CONSTRAINT booking_workspace_no_overlap EXCLUDE USING gist
        (workspace_id WITH =, tsrange(start_time, end_time) WITH &&)
        WHERE (status = 'ACTIVE' AND NOT archived),
    ADD CONSTRAINT booking_employee_no_overlap EXCLUDE USING gist
        (employee_id WITH =, tsrange(start_time, end_time) WITH &&)
        WHERE (status = 'ACTIVE' AND NOT archived);
CREATE SEQUENCE booking_number_seq MAXVALUE 9999999999999999;
CREATE INDEX idx_booking_created_at ON booking(created_at);

-- Старые refresh-сессии содержат JWT в открытом виде.
DELETE FROM refresh_session;

-- В черновой версии пароль этой временной учётной записи был общим для всех установок.
UPDATE user_account
SET active = FALSE
WHERE email = 'admin@example.org' AND role = 'ADMIN' AND temporary_password;
