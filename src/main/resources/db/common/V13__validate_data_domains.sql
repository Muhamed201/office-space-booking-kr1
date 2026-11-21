ALTER TABLE workspace
    ADD CONSTRAINT workspace_valid_code CHECK (code ~ '^[A-Z]{1,3}-[0-9]{1,3}$') NOT VALID,
    ADD CONSTRAINT workspace_valid_capacity CHECK
        (capacity BETWEEN 1 AND 100 AND (type = 'MEETING_ROOM' OR capacity = 1)) NOT VALID;

ALTER TABLE booking
    ADD CONSTRAINT booking_valid_priority CHECK (priority BETWEEN 1 AND 10) NOT VALID,
    ADD CONSTRAINT booking_valid_version CHECK (version >= 0) NOT VALID;

ALTER TABLE booking_note
    ADD CONSTRAINT booking_note_nonblank_content CHECK (content ~ '[^[:space:]]') NOT VALID;

ALTER TABLE user_account
    ADD CONSTRAINT user_account_nonblank_name CHECK (name ~ '[^[:space:]]') NOT VALID,
    ADD CONSTRAINT user_account_auth_version_nonnegative CHECK (auth_version >= 0) NOT VALID;
