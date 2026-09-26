package ru.mirea.officebooking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    public EmployeeService(EmployeeRepository employeeRepository, BookingRepository bookingRepository) {
        this.employeeRepository = employeeRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
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
