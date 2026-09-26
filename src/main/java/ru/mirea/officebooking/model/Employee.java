package ru.mirea.officebooking.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;
import ru.mirea.officebooking.exception.ValidationException;

public class Employee {

    public static final int MAX_FULL_NAME_LENGTH = 150;
    public static final int MAX_EMAIL_LENGTH = 150;
    public static final int MAX_DEPARTMENT_LENGTH = 100;
    public static final int MAX_POSITION_LENGTH = 100;
    public static final int MAX_YEARS_SINCE_HIRE = 50;

    private static final String NAME_WORD = "[А-ЯЁ][а-яё]+(?:-[А-ЯЁ][а-яё]+)?";
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^" + NAME_WORD + "(?: " + NAME_WORD + "){2}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final Long id;
    private final String fullName;
    private final String email;
    private final String department;
    private final String position;
    private final LocalDate hireDate;
    private final boolean blocked;
    private final EmployeeRole role;

    public Employee(Long id, String fullName, String email, String department, String position,
                    LocalDate hireDate, boolean blocked, EmployeeRole role) {
        this.id = Validation.optionalPositiveId(id, "id сотрудника");
        this.fullName = normalizeFullName(fullName);
        this.email = normalizeEmail(email);
        this.department = normalizeDepartment(department);
        this.position = normalizePosition(position);
        this.hireDate = validateHireDate(hireDate);
        this.blocked = blocked;
        this.role = Validation.requireNotNull(role, "роль");
    }

    public static String normalizeFullName(String value) {
        String name = Validation.requireText(value, "ФИО", MAX_FULL_NAME_LENGTH);
        if (!FULL_NAME_PATTERN.matcher(name).matches()) {
            throw new ValidationException(
                    "ФИО должно состоять из трёх слов в формате «Фамилия Имя Отчество»: "
                            + "русские буквы, каждое слово с заглавной буквы, между словами ровно один пробел");
        }
        return name;
    }

    public static String normalizeEmail(String value) {
        String mail = Validation.requireText(value, "Email", MAX_EMAIL_LENGTH).toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(mail).matches()) {
            throw new ValidationException("Неправильный формат почты, ожидается вид name@domain.ru: " + mail);
        }
        return mail;
    }

    public static String normalizeDepartment(String value) {
        return Validation.requireText(value, "Отдел", MAX_DEPARTMENT_LENGTH);
    }

    public static String normalizePosition(String value) {
        return Validation.optionalText(value, "Должность", MAX_POSITION_LENGTH);
    }

    public static LocalDate validateHireDate(LocalDate value) {
        Validation.requireNotNull(value, "Дата приёма");
        LocalDate today = LocalDate.now();
        if (value.isAfter(today)) {
            throw new ValidationException("Дата приёма не может быть в будущем: " + value);
        }
        if (value.isBefore(today.minusYears(MAX_YEARS_SINCE_HIRE))) {
            throw new ValidationException(
                    "Дата приёма не может быть раньше чем %d лет назад: %s".formatted(MAX_YEARS_SINCE_HIRE, value));
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public String getPosition() {
        return position;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public Employee withId(long newId) {
        return new Employee(newId, fullName, email, department, position, hireDate, blocked, role);
    }

    public Employee withBlocked(boolean newBlocked) {
        return new Employee(id, fullName, email, department, position, hireDate, newBlocked, role);
    }

    @Override
    public String toString() {
        return "#%d %s <%s> %s, %s%s".formatted(id, fullName, email, department, role, blocked ? " [ЗАБЛОКИРОВАН]" : "");
    }
}
