INSERT INTO employee (full_name, email, department, position, hire_date, blocked, role)
VALUES ('Орлова Анна Викторовна', 'orlova@company.ru', 'Администрация', 'Офис-менеджер', '2020-01-15', FALSE,
        'OFFICE_MANAGER')
ON CONFLICT (email) DO NOTHING;
