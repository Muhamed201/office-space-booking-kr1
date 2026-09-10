CREATE TABLE IF NOT EXISTS employee
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name  VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    position   VARCHAR(100),
    hire_date  DATE         NOT NULL CHECK (hire_date <= CURRENT_DATE),
    blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
    role       VARCHAR(20)  NOT NULL CHECK ( role IN ('EMPLOYEE', 'OFFICE_MANAGER'))

);

CREATE TABLE IF NOT EXISTS workspace
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(20) NOT NULL UNIQUE,
    floor       INT         NOT NULL CHECK (floor BETWEEN -2 AND 50),
    type        VARCHAR(20) NOT NULL CHECK (type IN ('OPEN_DESK', 'MEETING_ROOM', 'PHONE_BOOTH')),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'DECOMMISSIONED')),
    has_monitor BOOLEAN     NOT NULL DEFAULT FALSE,
    capacity    INT         NOT NULL CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS booking
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id  BIGINT                   NOT NULL REFERENCES employee (id) ON DELETE RESTRICT,
    workspace_id BIGINT                   NOT NULL REFERENCES workspace (id) ON DELETE RESTRICT,
    start_time   TIMESTAMP                NOT NULL,
    end_time     TIMESTAMP                NOT NULL,
    status       VARCHAR(20)              NOT NULL CHECK ( status IN ('ACTIVE', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    attendees    INT CHECK (attendees IS NULL OR attendees > 0),
    purpose      VARCHAR(255),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (start_time < end_time)


);