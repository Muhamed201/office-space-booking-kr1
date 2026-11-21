package ru.mirea.officebooking.service;

import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.officebooking.domain.*;
import ru.mirea.officebooking.error.ConflictException;
import ru.mirea.officebooking.error.EntityNotFoundException;
import ru.mirea.officebooking.filter.Pages;
import ru.mirea.officebooking.repository.*;
import ru.mirea.officebooking.security.CurrentUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AdminUserService {
    public record Create(@NotBlank @Size(max = 150) @Pattern(regexp = "[\\P{Cc}]*") String name, @NotBlank @Email @Size(max = 150) String email,
                         @NotNull Role role, @NotBlank @Size(min = 8, max = 72) String temporaryPassword) {}
    public record Update(@NotBlank @Size(max = 150) @Pattern(regexp = "[\\P{Cc}]*") String name, @NotBlank @Email @Size(max = 150) String email,
                         @NotNull Role role, @NotNull Boolean active) {}
    public record Reset(@NotBlank @Size(min = 8, max = 72) String temporaryPassword) {}
    public record Account(UUID id, UUID employeeId, String name, String email, Role role, boolean active,
                          boolean temporaryPassword, LocalDateTime createdAt) {}

    private final UserAccountRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final AuthService auth;
    private final AuditService audit;

    public AdminUserService(UserAccountRepository users, EmployeeRepository employees, PasswordEncoder encoder,
                            AuthService auth, AuditService audit) {
        this.users = users;
        this.employees = employees;
        this.encoder = encoder;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<Account> list(Pageable pageable) {
        return users.findAll(Pages.sorted(pageable, Set.of("id", "name", "email", "role", "createdAt"),
                Sort.by("name"))).map(this::dto);
    }

    public Account create(Create input) {
        AuthService.validatePassword(input.temporaryPassword());
        String email = normalize(input.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        Employee employee = employees.findByEmail(email).orElseGet(() -> employees.save(
                new Employee(input.name().strip(), email, "Офис", null, LocalDate.now())));
        employee.setFullName(input.name().strip());
        UserAccount user = users.save(new UserAccount(employee, encoder.encode(input.temporaryPassword()), input.role()));
        audit.logEvent(EventType.CREATE, "UserAccount", user.getId(), "Создана учётная запись");
        return dto(user);
    }

    public Account update(UUID id, Update input) {
        UserAccount user = find(id);
        if (id.equals(CurrentUser.get().id()) && (!input.active() || input.role() != Role.ADMIN)) {
            throw new ConflictException("Нельзя отключить свою учётную запись или снять с неё роль администратора");
        }
        String email = normalize(input.email());
        var conflictingEmployee = employees.findByEmail(email);
        if (conflictingEmployee.isPresent() && !conflictingEmployee.get().getId().equals(user.getEmployee().getId())) {
            throw new ConflictException("Этот email уже используется");
        }
        if (!user.getRole().equals(input.role()) || !user.getActive().equals(input.active())
                || !user.getEmail().equals(email)) {
            auth.invalidateSessions(user);
        }
        user.setName(input.name().strip());
        user.setEmail(email);
        user.getEmployee().setFullName(input.name().strip());
        user.getEmployee().setEmail(email);
        user.setRole(input.role());
        user.setActive(input.active());
        audit.logEvent(EventType.UPDATE, "UserAccount", id, "Учётная запись обновлена");
        return dto(user);
    }

    public void reset(UUID id, Reset input) {
        auth.setPassword(find(id), input.temporaryPassword(), true);
        audit.logEvent(EventType.UPDATE, "UserAccount", id, "Установлен временный пароль");
    }

    private UserAccount find(UUID id) {
        return users.findLockedById(id).orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Account dto(UserAccount user) {
        return new Account(user.getId(), user.getEmployee().getId(), user.getName(), user.getEmail(),
                user.getRole(), user.getActive(), user.getTemporaryPassword(), user.getCreatedAt());
    }
}
