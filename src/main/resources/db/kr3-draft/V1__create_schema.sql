-- Таблица сотрудников
CREATE TABLE IF NOT EXISTS employee
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name  VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    position   VARCHAR(100),
    hire_date  DATE         NOT NULL CHECK (hire_date <= CURRENT_DATE),
    blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employee_email ON employee(email);

-- Таблица рабочих мест
CREATE TABLE IF NOT EXISTS workspace
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(20) NOT NULL UNIQUE,
    floor       INT         NOT NULL CHECK (floor BETWEEN -2 AND 50),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('OPEN_DESK', 'MEETING_ROOM', 'PHONE_BOOTH')),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'DECOMMISSIONED')),
    has_monitor BOOLEAN     NOT NULL DEFAULT FALSE,
    capacity    INT         NOT NULL CHECK (capacity > 0),
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workspace_code ON workspace(code);

-- Таблица бронирований
CREATE TABLE IF NOT EXISTS booking
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_number     VARCHAR(20) NOT NULL UNIQUE,
    employee_id        UUID        NOT NULL REFERENCES employee(id) ON DELETE RESTRICT,
    workspace_id       UUID        NOT NULL REFERENCES workspace(id) ON DELETE RESTRICT,
    start_time         TIMESTAMP   NOT NULL,
    end_time           TIMESTAMP   NOT NULL,
    status             VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    attendees          INT         CHECK (attendees IS NULL OR attendees > 0),
    purpose            VARCHAR(500),
    is_recurring       BOOLEAN     NOT NULL DEFAULT FALSE,
    requires_equipment BOOLEAN     NOT NULL DEFAULT FALSE,
    priority           INT         NOT NULL DEFAULT 1,
    version            BIGINT      NOT NULL DEFAULT 0,
    archived           BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (start_time < end_time)
);

CREATE INDEX idx_booking_status ON booking(status);
CREATE INDEX idx_booking_archived ON booking(archived);
CREATE INDEX idx_booking_employee ON booking(employee_id);
CREATE INDEX idx_booking_workspace ON booking(workspace_id);
CREATE INDEX idx_booking_number ON booking(booking_number);
