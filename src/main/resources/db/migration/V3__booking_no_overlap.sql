CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE booking
    ADD CONSTRAINT booking_workspace_no_overlap
        EXCLUDE USING gist (
        workspace_id WITH =,
        tsrange(start_time, end_time) WITH &&
        ) WHERE (status = 'ACTIVE');

ALTER TABLE booking
    ADD CONSTRAINT booking_employee_no_overlap
        EXCLUDE USING gist (
        employee_id WITH =,
        tsrange(start_time, end_time) WITH &&
        ) WHERE (status = 'ACTIVE');
