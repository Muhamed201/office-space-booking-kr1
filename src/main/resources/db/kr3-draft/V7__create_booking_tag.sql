-- Промежуточная таблица для связи booking <-> tag
CREATE TABLE IF NOT EXISTS booking_tag (
    booking_id UUID NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (booking_id, tag_id)
);
