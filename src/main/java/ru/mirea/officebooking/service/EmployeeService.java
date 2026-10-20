package ru.mirea.officebooking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import ru.mirea.officebooking.exception.AccessDeniedException;
import ru.mirea.officebooking.exception.EntityNotFoundException;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.model.Validation;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.repository.EmployeeRepository;

public class EmployeeService {

    private static final String ENTITY_NAME = "Сотрудник";

    private final EmployeeRepository employeeRepository;
    private final BookingRepository bookingRepository;
    private final AccessPolicy accessPolicy;

    public EmployeeService(EmployeeRepository employeeRepository, BookingRepository bookingRepository) {
        this.employeeRepository = employeeRepository;
        this.bookingRepository = bookingRepository;
        this.accessPolicy = new AccessPolicy(employeeRepository);
    }

    public Employee login(String email) {
        String normalized = Employee.normalizeEmail(email);
        Employee employee = employeeRepository.findByEmail(normalized)
                .orElseThrow(() -> new AccessDeniedException("Сотрудник с email " + normalized + " не найден"));
        if (employee.isBlocked()) {
            throw new AccessDeniedException("Сотрудник %s заблокирован, вход запрещён".formatted(employee.getFullName()));
        }
        return employee;
    }

    public Employee createAs(long actorId, String fullName, String email, String department, String position,
                             LocalDate hireDate, EmployeeRole role) {
        accessPolicy.requireManager(actorId, "добавлять сотрудников");
        return create(fullName, email, department, position, hireDate, role);
    }

    public Employee updateAs(long actorId, long id, String fullName, String email, String department, String position,
                             LocalDate hireDate, EmployeeRole role) {
        accessPolicy.requireManager(actorId, "изменять данные сотрудников");
        return update(id, fullName, email, department, position, hireDate, role);
    }

    public Employee setBlockedAs(long actorId, long id, boolean blocked) {
        accessPolicy.requireManager(actorId, "блокировать сотрудников");
        if (actorId == id) {
            throw new ValidationException("Нельзя заблокировать самого себя");
        }
        return setBlocked(id, blocked);
    }

    public void deleteAs(long actorId, long id) {
        accessPolicy.requireManager(actorId, "удалять сотрудников");
        if (actorId == id) {
            throw new ValidationException("Нельзя удалить самого себя");
        }
        delete(id);
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public List<Employee> findAvailableForBooking() {
        return employeeRepository.findAll().stream().filter(e -> !e.isBlocked()).toList();
    }

    public Employee findById(long id) {
        Validation.requirePositiveId(id, "id сотрудника");
        return employeeRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.forId(ENTITY_NAME, id));
    }

    public Employee create(String fullName, String email, String department, String position,
                           LocalDate hireDate, EmployeeRole role) {
        Employee candidate = new Employee(null, fullName, email, department, position, hireDate, false, role);
        ensureEmailFree(candidate.getEmail(), null);
        return employeeRepository.save(candidate);
    }

    public Employee update(long id, String fullName, String email, String department, String position,
                           LocalDate hireDate, EmployeeRole role) {
        Employee existing = findById(id);
        Employee updated = new Employee(id, fullName, email, department, position, hireDate,
                existing.isBlocked(), role);
        ensureEmailFree(updated.getEmail(), id);
        employeeRepository.update(updated);
        return updated;
    }

    public Employee setBlocked(long id, boolean blocked) {
        Employee existing = findById(id);
        if (existing.isBlocked() == blocked) {
            throw new ValidationException(
                    "Сотрудник %s уже %s".formatted(existing.getFullName(), blocked ? "заблокирован" : "разблокирован"));
        }
        Employee updated = existing.withBlocked(blocked);
        employeeRepository.update(updated);
        return updated;
    }

    public void delete(long id) {
        Employee existing = findById(id);
        long bookings = bookingRepository.countByEmployeeId(id);
        if (bookings > 0) {
            throw new ValidationException(
                    "Нельзя удалить сотрудника %s: на него оформлено броней — %d".formatted(existing.getFullName(), bookings));
        }
        employeeRepository.deleteById(id);
    }

    private void ensureEmailFree(String email, Long editingId) {
        Optional<Employee> sameEmail = employeeRepository.findByEmail(email);
        if (sameEmail.isPresent() && !sameEmail.get().getId().equals(editingId)) {
            throw new ValidationException("Сотрудник с email " + email + " уже существует");
        }
    }
}
