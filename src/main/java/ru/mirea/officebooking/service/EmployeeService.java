package ru.mirea.officebooking.service;

import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.repository.EmployeeRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee create(Employee employee) {
        validate(employee);
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Сотрудник с таким email уже существует");
        }
        return employeeRepository.save(employee);
    }

    public Optional<Employee> findById(Long id) {
        return employeeRepository.findById(id);
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee update(Employee employee) {
        if (employee.getId() == null) {
            throw new IllegalArgumentException("ID сотрудника не указан для обновления");
        }
        validate(employee);
        if (employeeRepository.existsByEmail(employee.getEmail(), employee.getId())) {
            throw new IllegalArgumentException("Сотрудник с таким email уже существует");
        }
        return employeeRepository.update(employee);
    }

    public void delete(Long id) {
        if (employeeRepository.hasBookings(id)) {
            throw new IllegalStateException("Нельзя удалить сотрудника, у которого есть бронирования");
        }
        employeeRepository.deleteById(id);
    }

    public void block(Long id, boolean blocked) {
        Optional<Employee> opt = employeeRepository.findById(id);
        if (opt.isPresent()) {
            Employee emp = opt.get();
            emp.setBlocked(blocked);
            employeeRepository.update(emp);
        } else {
            throw new IllegalArgumentException("Сотрудник не найден");
        }
    }

    private void validate(Employee employee) {
        if (employee.getFullName() == null || employee.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("ФИО не может быть пустым");
        }
        if (employee.getEmail() == null || !employee.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Некорректный email");
        }
        if (employee.getDepartment() == null || employee.getDepartment().trim().isEmpty()) {
            throw new IllegalArgumentException("Департамент не может быть пустым");
        }
        if (employee.getHireDate() == null || employee.getHireDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Дата найма не может быть в будущем");
        }
        if (employee.getRole() == null) {
            throw new IllegalArgumentException("Роль сотрудника обязательна");
        }

        // Проверка возраста >= 14 лет
        if (employee.getBirthDate() != null) {
            int age = Period.between(employee.getBirthDate(), LocalDate.now()).getYears();
            if (age < 14) {
                throw new IllegalArgumentException("Возраст сотрудника должен быть не менее 14 лет");
            }
            if (employee.getHireDate() != null) {
                int ageAtHire = Period.between(employee.getBirthDate(), employee.getHireDate()).getYears();
                if (ageAtHire < 14) {
                    throw new IllegalArgumentException("Сотрудник не может быть нанят в возрасте младше 14 лет");
                }
            }
        }
    }
}