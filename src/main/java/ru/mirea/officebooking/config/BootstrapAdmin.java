package ru.mirea.officebooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.officebooking.domain.Employee;
import ru.mirea.officebooking.domain.Role;
import ru.mirea.officebooking.domain.UserAccount;
import ru.mirea.officebooking.repository.EmployeeRepository;
import ru.mirea.officebooking.repository.UserAccountRepository;
import ru.mirea.officebooking.service.AuthService;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class BootstrapAdmin implements CommandLineRunner {
    private final UserAccountRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final String email;
    private final String name;
    private final String password;

    public BootstrapAdmin(UserAccountRepository users, EmployeeRepository employees, PasswordEncoder encoder,
                          @Value("${app.bootstrap.email}") String email,
                          @Value("${app.bootstrap.name}") String name,
                          @Value("${app.bootstrap.password}") String password) {
        this.users = users;
        this.employees = employees;
        this.encoder = encoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.name = name.trim();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByRoleAndActiveTrue(Role.ADMIN)) {
            return;
        }
        if (password.isBlank()) {
            throw new IllegalStateException("Укажите BOOTSTRAP_ADMIN_PASSWORD для первоначального запуска");
        }
        AuthService.validatePassword(password);
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            var admin = existing.get();
            if (admin.getRole() != Role.ADMIN || encoder.matches(password, admin.getPasswordHash())) {
                throw new IllegalStateException("Для восстановления администратора укажите новый временный пароль");
            }
            admin.setPasswordHash(encoder.encode(password));
            admin.setActive(true);
            admin.setTemporaryPassword(true);
            admin.resetFailedAttempts();
            admin.setAuthVersion(admin.getAuthVersion() + 1);
            return;
        }
        var employee = employees.findByEmail(email).orElseGet(() -> employees.save(
                new Employee(name, email, "Администрация", "Системный администратор", LocalDate.now())));
        users.save(new UserAccount(employee, encoder.encode(password), Role.ADMIN));
    }
}
