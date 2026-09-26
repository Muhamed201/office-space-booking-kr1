package ru.mirea.officebooking.ui;

import java.time.LocalDate;
import java.util.List;
import ru.mirea.officebooking.exception.AppException;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.service.EmployeeService;

public class EmployeeMenu {

    private final EmployeeService employeeService;
    private final ConsoleReader reader;

    public EmployeeMenu(EmployeeService employeeService, ConsoleReader reader) {
        this.employeeService = employeeService;
        this.reader = reader;
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""

                    --- Сотрудники ---
                    1. Список всех
                    2. Найти по id
                    3. Добавить
                    4. Изменить
                    5. Удалить
                    6. Заблокировать / разблокировать
                    0. Назад""");
            int choice = reader.readInt("Выбор: ");
            try {
                switch (choice) {
                    case 1 -> listAll();
                    case 2 -> findById();
                    case 3 -> create();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> toggleBlocked();
                    case 0 -> inMenu = false;
                    default -> System.out.println("Нет такого пункта меню: " + choice);
                }
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void listAll() {
        List<Employee> employees = employeeService.findAll();
        if (employees.isEmpty()) {
            System.out.println("Сотрудников пока нет.");
            return;
        }
        employees.forEach(System.out::println);
    }

    private void findById() {
        System.out.println(employeeService.findById(reader.readId("id сотрудника: ")));
    }

    private void create() {
        String fullName = reader.readUntilValid(
                () -> reader.readLine("ФИО (Фамилия Имя Отчество): "), Employee::normalizeFullName);
        String email = reader.readUntilValid(() -> reader.readLine("Email: "), Employee::normalizeEmail);
        String department = reader.readUntilValid(() -> reader.readLine("Отдел: "), Employee::normalizeDepartment);
        String position = reader.readUntilValid(
                () -> reader.readLine("Должность (Enter — пропустить): "), Employee::normalizePosition);
        LocalDate hireDate = reader.readUntilValid(() -> reader.readDate("Дата приёма"), Employee::validateHireDate);
        EmployeeRole role = reader.readEnum(EmployeeRole.class, "Роль:");
        Employee created = employeeService.create(fullName, email, department, position, hireDate, role);
        System.out.println("Создан: " + created);
    }

    private void update() {
        long id = reader.readId("id сотрудника: ");
        Employee current = employeeService.findById(id);
        System.out.println("Текущие данные: " + current);
        String fullName = reader.readUntilValid(
                () -> reader.readLine("Новое ФИО (Фамилия Имя Отчество): "), Employee::normalizeFullName);
        String email = reader.readUntilValid(() -> reader.readLine("Новый email: "), Employee::normalizeEmail);
        String department = reader.readUntilValid(() -> reader.readLine("Новый отдел: "), Employee::normalizeDepartment);
        String position = reader.readUntilValid(
                () -> reader.readLine("Новая должность (Enter — очистить): "), Employee::normalizePosition);
        LocalDate hireDate = reader.readUntilValid(() -> reader.readDate("Новая дата приёма"), Employee::validateHireDate);
        EmployeeRole role = reader.readEnum(EmployeeRole.class, "Новая роль:");
        Employee updated = employeeService.update(id, fullName, email, department, position, hireDate, role);
        System.out.println("Обновлён: " + updated);
    }

    private void delete() {
        long id = reader.readId("id сотрудника: ");
        Employee employee = employeeService.findById(id);
        if (!reader.readYesNo("Удалить сотрудника " + employee.getFullName() + "?")) {
            System.out.println("Удаление отменено.");
            return;
        }
        employeeService.delete(id);
        System.out.println("Сотрудник id=" + id + " удалён.");
    }

    private void toggleBlocked() {
        long id = reader.readId("id сотрудника: ");
        boolean blocked = reader.readYesNo("Заблокировать (д) или разблокировать (н)?");
        System.out.println("Обновлён: " + employeeService.setBlocked(id, blocked));
    }
}
