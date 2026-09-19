INSERT INTO employee (full_name, email, department, position, hire_date, blocked, role)
VALUES
    ('Иванов Иван Иванович',      'ivanov@company.ru',    'Разработка', 'Backend-разработчик',  '2022-03-15', FALSE, 'EMPLOYEE'),
    ('Петрова Мария Сергеевна',   'petrova@company.ru',   'HR',         'HR-менеджер',          '2021-11-01', TRUE,  'OFFICE_MANAGER'),
    ('Сидоров Алексей Викторович','sidorov@company.ru',   'Маркетинг',  'Маркетолог',           '2023-01-10', FALSE, 'EMPLOYEE'),
    ('Кузнецова Ольга Дмитриевна','kuznetsova@company.ru','Разработка', 'Frontend-разработчик', '2020-06-20', FALSE, 'EMPLOYEE'),
    ('Смирнов Дмитрий Александрович','smirnov@company.ru','Маркетинг',  'SMM-специалист',       '2023-09-05', FALSE, 'EMPLOYEE'),
    ('Волкова Екатерина Павловна','volkova@company.ru',   'HR',         'Рекрутер',             '2024-02-12', FALSE, 'EMPLOYEE');
INSERT INTO workspace (code, floor, type, status, has_monitor, capacity)
VALUES
    ('A-01', 2, 'OPEN_DESK',    'AVAILABLE',     TRUE,  1),
    ('A-02', 2, 'OPEN_DESK',    'AVAILABLE',     TRUE,  1),
    ('A-03', 2, 'OPEN_DESK',    'MAINTENANCE',   FALSE, 1),
    ('A-04', 2, 'OPEN_DESK',    'AVAILABLE',     FALSE, 1),
    ('B-01', 2, 'PHONE_BOOTH',  'AVAILABLE',     FALSE, 1),
    ('A-05', 3, 'OPEN_DESK',    'AVAILABLE',     TRUE,  1),
    ('A-06', 3, 'OPEN_DESK',    'AVAILABLE',     TRUE,  1),
    ('A-07', 3, 'OPEN_DESK',    'AVAILABLE',     TRUE,  1),
    ('M-01', 3, 'MEETING_ROOM', 'AVAILABLE',     TRUE,  4),
    ('M-02', 3, 'MEETING_ROOM', 'MAINTENANCE',   TRUE,  6),
    ('B-02', 4, 'PHONE_BOOTH',  'DECOMMISSIONED',FALSE, 1),
    ('M-03', 4, 'MEETING_ROOM', 'AVAILABLE',     TRUE,  12);

INSERT INTO booking (employee_id, workspace_id, start_time, end_time, status, attendees, purpose)
VALUES
    ((SELECT id FROM employee WHERE email = 'ivanov@company.ru'),     (SELECT id FROM workspace WHERE code = 'A-01'), '2026-08-10 09:00', '2026-08-10 17:00', 'COMPLETED', NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'petrova@company.ru'),    (SELECT id FROM workspace WHERE code = 'M-01'), '2026-08-11 10:00', '2026-08-11 12:00', 'COMPLETED', 3,    'Совещание отдела'),
    ((SELECT id FROM employee WHERE email = 'sidorov@company.ru'),    (SELECT id FROM workspace WHERE code = 'B-01'), '2026-08-12 09:00', '2026-08-12 10:00', 'NO_SHOW',   NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'kuznetsova@company.ru'), (SELECT id FROM workspace WHERE code = 'A-02'), '2026-08-13 09:00', '2026-08-13 18:00', 'COMPLETED', NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'smirnov@company.ru'),    (SELECT id FROM workspace WHERE code = 'A-01'), '2026-08-14 09:00', '2026-08-14 17:00', 'COMPLETED', NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'volkova@company.ru'),    (SELECT id FROM workspace WHERE code = 'M-03'), '2026-08-17 14:00', '2026-08-17 16:00', 'COMPLETED', 8,    'Планирование квартала'),
    ((SELECT id FROM employee WHERE email = 'ivanov@company.ru'),     (SELECT id FROM workspace WHERE code = 'A-01'), '2026-09-01 09:00', '2026-09-01 17:00', 'CANCELLED', NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'kuznetsova@company.ru'), (SELECT id FROM workspace WHERE code = 'B-01'), '2026-08-20 10:00', '2026-08-20 11:00', 'CANCELLED', NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'sidorov@company.ru'),    (SELECT id FROM workspace WHERE code = 'A-04'), '2026-08-25 09:00', '2026-08-25 13:00', 'NO_SHOW',   NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'ivanov@company.ru'),     (SELECT id FROM workspace WHERE code = 'A-05'), '2026-09-20 09:00', '2026-09-20 17:00', 'ACTIVE',    NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'kuznetsova@company.ru'), (SELECT id FROM workspace WHERE code = 'A-06'), '2026-09-22 09:00', '2026-09-22 18:00', 'ACTIVE',    NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'smirnov@company.ru'),    (SELECT id FROM workspace WHERE code = 'M-01'), '2026-09-23 10:00', '2026-09-23 12:00', 'ACTIVE',    4,    'Ретроспектива'),
    ((SELECT id FROM employee WHERE email = 'volkova@company.ru'),    (SELECT id FROM workspace WHERE code = 'A-07'), '2026-09-25 09:00', '2026-09-25 17:00', 'ACTIVE',    NULL, NULL),
    ((SELECT id FROM employee WHERE email = 'volkova@company.ru'),    (SELECT id FROM workspace WHERE code = 'A-01'), '2026-09-29 09:00', '2026-09-29 17:00', 'ACTIVE',    NULL, NULL);