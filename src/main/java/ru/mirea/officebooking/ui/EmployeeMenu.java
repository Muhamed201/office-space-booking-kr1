package ru.mirea.officebooking.ui;

import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.service.EmployeeService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class EmployeeMenu {
    private final EmployeeService employeeService;
    private final Scanner scanner;

    public EmployeeMenu(EmployeeService employeeService, Scanner scanner) {
        this.employeeService = employeeService;
        this.scanner = scanner;
    }

    public void show() {
        while (true) {
            System.out.println("\n=== Меню управления сотрудниками ===");
            System.out.println("1. Список всех сотрудников");
            System.out.println("2. Найти сотрудника по ID");
            System.out.println("3. Добавить сотрудника");
            System.out.println("4. Редактировать сотрудника");
            System.out.println("5. Заблокировать / Разблокировать");
            System.out.println("6. Удалить сотрудника");
            System.out.println("0. Вернуться назад");
            System.out.print("Ваш выбор: ");

            String choice = scanner.nextLine();
            try {
                switch (choice) {
                    case "1" -> showAll();
                    case "2" -> findById();
                    case "3" -> createEmployee();
                    case "4" -> updateEmployee();
                    case "5" -> toggleBlock();
                    case "6" -> deleteEmployee();
                    case "0" -> { return; }
                    default -> System.out.println("Нет такого пункта.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Ошибка валидации / бизнес-логики: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Произошла ошибка: " + e.getMessage());
            }
        }
    }

    private void showAll() {
        List<Employee> employees = employeeService.findAll();
        if (employees.isEmpty()) {
            System.out.println("Список сотрудников пуст.");
            return;
        }
        employees.forEach(System.out::println);
    }

    private void findById() {
        System.out.print("Введите ID сотрудника: ");
        Long id = Long.parseLong(scanner.nextLine());
        Optional<Employee> emp = employeeService.findById(id);
        System.out.println(emp.orElse(null));
    }

    private void createEmployee() {
        Employee emp = readEmployeeFromConsole(null);
        Employee saved = employeeService.create(emp);
        System.out.println("Сотрудник успешно создан: " + saved);
    }

    private void updateEmployee() {
        System.out.print("Введите ID сотрудника для редактирования: ");
        Long id = Long.parseLong(scanner.nextLine());
        Optional<Employee> existing = employeeService.findById(id);
        if (existing.isEmpty()) {
            System.out.println("Сотрудник не найден.");
            return;
        }
        Employee emp = readEmployeeFromConsole(existing.get());
        Employee updated = employeeService.update(emp);
        System.out.println("Сотрудник обновлён: " + updated);
    }

    private void toggleBlock() {
        System.out.print("Введите ID сотрудника: ");
        Long id = Long.parseLong(scanner.nextLine());
        System.out.print("Заблокировать (1) или Разблокировать (0)? ");
        String blockStr = scanner.nextLine();
        boolean blocked = "1".equals(blockStr);
        employeeService.block(id, blocked);
        System.out.println("Статус изменен.");
    }

    private void deleteEmployee() {
        System.out.print("Введите ID сотрудника для удаления: ");
        Long id = Long.parseLong(scanner.nextLine());
        employeeService.delete(id);
        System.out.println("Сотрудник удалён.");
    }

    private Employee readEmployeeFromConsole(Employee existing) {
        Employee emp = (existing != null) ? existing : new Employee();

        System.out.print("ФИО [" + (existing != null ? existing.getFullName() : "") + "]: ");
        String fullName = scanner.nextLine();
        if (!fullName.trim().isEmpty()) emp.setFullName(fullName);
        else if (existing == null) throw new IllegalArgumentException("ФИО не может быть пустым");

        System.out.print("Email [" + (existing != null ? existing.getEmail() : "") + "]: ");
        String email = scanner.nextLine();
        if (!email.trim().isEmpty()) emp.setEmail(email);
        else if (existing == null) throw new IllegalArgumentException("Email не может быть пустым");

        System.out.print("Департамент [" + (existing != null ? existing.getDepartment() : "") + "]: ");
        String dept = scanner.nextLine();
        if (!dept.trim().isEmpty()) emp.setDepartment(dept);
        else if (existing == null) throw new IllegalArgumentException("Департамент не может быть пустым");

        System.out.print("Должность [" + (existing != null ? existing.getPosition() : "") + "]: ");
        String pos = scanner.nextLine();
        if (!pos.trim().isEmpty()) emp.setPosition(pos);

        System.out.print("Дата найма (YYYY-MM-DD) [" + (existing != null ? existing.getHireDate() : "") + "]: ");
        String hireDateStr = scanner.nextLine();
        if (!hireDateStr.trim().isEmpty()) {
            emp.setHireDate(LocalDate.parse(hireDateStr));
        } else if (existing == null) {
            throw new IllegalArgumentException("Дата найма обязательна");
        }

        System.out.print("Дата рождения (YYYY-MM-DD) [" + (existing != null && existing.getBirthDate() != null ? existing.getBirthDate() : "") + "]: ");
        String birthDateStr = scanner.nextLine();
        if (!birthDateStr.trim().isEmpty()) {
            emp.setBirthDate(LocalDate.parse(birthDateStr));
        }

        System.out.print("Роль (EMPLOYEE / OFFICE_MANAGER) [" + (existing != null ? existing.getRole() : "") + "]: ");
        String roleStr = scanner.nextLine();
        if (!roleStr.trim().isEmpty()) {
            emp.setRole(EmployeeRole.valueOf(roleStr.toUpperCase()));
        } else if (existing == null) {
            throw new IllegalArgumentException("Роль обязательна");
        }

        return emp;
    }
}