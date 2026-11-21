ALTER TABLE booking DROP CONSTRAINT booking_workspace_no_overlap,
                    DROP CONSTRAINT booking_employee_no_overlap,
                    DROP CONSTRAINT booking_employee_id_fkey,
                    DROP CONSTRAINT booking_workspace_id_fkey;

ALTER TABLE employee ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE workspace ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE booking ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid(),
                    ADD COLUMN employee_uuid UUID,
                    ADD COLUMN workspace_uuid UUID,
                    ADD COLUMN booking_number VARCHAR(20),
                    ADD COLUMN description VARCHAR(2000),
                    ADD COLUMN is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
                    ADD COLUMN requires_equipment BOOLEAN NOT NULL DEFAULT FALSE,
                    ADD COLUMN priority INT NOT NULL DEFAULT 1,
                    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
                    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE,
                    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE booking b
SET employee_uuid = e.uuid, workspace_uuid = w.uuid, booking_number = 'BK-' || b.id
FROM employee e, workspace w
WHERE b.employee_id = e.id AND b.workspace_id = w.id;

ALTER TABLE employee DROP COLUMN id,
                     ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                     ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE workspace DROP COLUMN id,
                      ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE booking DROP COLUMN id, DROP COLUMN employee_id, DROP COLUMN workspace_id,
                    ALTER COLUMN booking_number SET NOT NULL,
                    ADD UNIQUE (booking_number),
                    ALTER COLUMN purpose TYPE VARCHAR(500),
                    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'Europe/Moscow';

ALTER TABLE employee RENAME COLUMN uuid TO id;
ALTER TABLE workspace RENAME COLUMN uuid TO id;
ALTER TABLE booking RENAME COLUMN uuid TO id;
ALTER TABLE booking RENAME COLUMN employee_uuid TO employee_id;
ALTER TABLE booking RENAME COLUMN workspace_uuid TO workspace_id;
ALTER TABLE employee ADD PRIMARY KEY (id), ALTER COLUMN role SET DEFAULT 'EMPLOYEE';
ALTER TABLE workspace ADD PRIMARY KEY (id);
ALTER TABLE booking ADD PRIMARY KEY (id),
                    ALTER COLUMN employee_id SET NOT NULL,
                    ALTER COLUMN workspace_id SET NOT NULL,
                    ADD FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE RESTRICT,
                    ADD FOREIGN KEY (workspace_id) REFERENCES workspace(id) ON DELETE RESTRICT,
                    ADD CONSTRAINT booking_workspace_no_overlap EXCLUDE USING gist
                        (workspace_id WITH =, tsrange(start_time, end_time) WITH &&)
                        WHERE (status = 'ACTIVE' AND NOT archived),
                    ADD CONSTRAINT booking_employee_no_overlap EXCLUDE USING gist
                        (employee_id WITH =, tsrange(start_time, end_time) WITH &&)
                        WHERE (status = 'ACTIVE' AND NOT archived);

CREATE SEQUENCE booking_number_seq MAXVALUE 9999999999999999;
CREATE INDEX idx_booking_created_at ON booking(created_at);
CREATE INDEX idx_booking_employee ON booking(employee_id);
CREATE INDEX idx_booking_workspace ON booking(workspace_id);
CREATE INDEX idx_booking_status ON booking(status);
