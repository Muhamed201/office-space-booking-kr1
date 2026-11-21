-- Таблица заметок к броням
CREATE TABLE IF NOT EXISTS booking_note (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES user_account(id),
    content VARCHAR(2000) NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_booking_note_booking ON booking_note(booking_id);
