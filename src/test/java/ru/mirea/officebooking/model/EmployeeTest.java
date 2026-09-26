package ru.mirea.officebooking.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.mirea.officebooking.exception.ValidationException;

class EmployeeTest {

    private static final LocalDate PAST = LocalDate.of(2022, 3, 15);

    private static Employee create(String fullName, String email, String department, LocalDate hireDate) {
        return new Employee(null, fullName, email, department, null, hireDate, false, EmployeeRole.EMPLOYEE);
    }

    @Test
    void validEmployeeIsCreatedAndNormalized() {
        Employee employee = new Employee(1L, "  Иванов Иван Иванович ", " Ivanov@Company.RU ", " Разработка ",
                "  ", PAST, false, EmployeeRole.EMPLOYEE);
        assertEquals("Иванов Иван Иванович", employee.getFullName());
        assertEquals("ivanov@company.ru", employee.getEmail());
        assertEquals("Разработка", employee.getDepartment());
        assertNull(employee.getPosition());
    }

    @Test
    void doubleBarrelledNamesAreAllowed() {
        Employee employee = create("Петров-Водкин Кузьма Сергеевич", "p@x.ru", "IT", PAST);
        assertEquals("Петров-Водкин Кузьма Сергеевич", employee.getFullName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Иванов Иван",
            "Иванов Иван Иванович Младший",
            "Иванов  Иван Иванович",
            "иванов иван иванович",
            "Ivanov Ivan Ivanovich",
            "Иванов Иван Иванович1",
            "Иванов И. И.",
            "Иванов-Иван-Иванович",
            "   ",
            ""
    })
    void wrongFullNameFormatIsRejected(String fullName) {
        assertThrows(ValidationException.class, () -> create(fullName, "a@b.ru", "IT", PAST));
    }

    @Test
    void nullFullNameIsRejected() {
        assertThrows(ValidationException.class, () -> create(null, "a@b.ru", "IT", PAST));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ivanov", "ivanov@company", "@company.ru", "iva nov@company.ru", "ivanov@company.r", ""})
    void wrongEmailIsRejected(String email) {
        assertThrows(ValidationException.class, () -> create("Иванов Иван Иванович", email, "IT", PAST));
    }

    @Test
    void nullEmailIsRejected() {
        assertThrows(ValidationException.class, () -> create("Иванов Иван Иванович", null, "IT", PAST));
    }

    @Test
    void blankDepartmentIsRejected() {
        assertThrows(ValidationException.class, () -> create("Иванов Иван Иванович", "a@b.ru", "  ", PAST));
        assertThrows(ValidationException.class, () -> create("Иванов Иван Иванович", "a@b.ru", null, PAST));
    }

    @Test
    void tooLongFieldsAreRejected() {
        String longDepartment = "Я".repeat(Employee.MAX_DEPARTMENT_LENGTH + 1);
        assertThrows(ValidationException.class, () -> create("Иванов Иван Иванович", "a@b.ru", longDepartment, PAST));
        String longPosition = "Я".repeat(Employee.MAX_POSITION_LENGTH + 1);
        assertThrows(ValidationException.class, () -> new Employee(null, "Иванов Иван Иванович", "a@b.ru", "IT",
                longPosition, PAST, false, EmployeeRole.EMPLOYEE));
    }

    @Test
    void hireDateInFutureIsRejected() {
        assertThrows(ValidationException.class,
                () -> create("Иванов Иван Иванович", "a@b.ru", "IT", LocalDate.now().plusDays(1)));
    }

    @Test
    void hireDateTodayIsAllowed() {
        assertEquals(LocalDate.now(), create("Иванов Иван Иванович", "a@b.ru", "IT", LocalDate.now()).getHireDate());
    }

    @Test
    void tooOldHireDateIsRejected() {
        assertThrows(ValidationException.class,
                () -> create("Иванов Иван Иванович", "a@b.ru", "IT", LocalDate.now().minusYears(51)));
    }

    @Test
    void nullHireDateAndNullRoleAreRejected() {
        assertThrows(ValidationException.class, () -> create("Иванов Иван Иванович", "a@b.ru", "IT", null));
        assertThrows(ValidationException.class, () -> new Employee(null, "Иванов Иван Иванович", "a@b.ru", "IT",
                null, PAST, false, null));
    }

    @Test
    void nonPositiveIdIsRejected() {
        assertThrows(ValidationException.class, () -> new Employee(0L, "Иванов Иван Иванович", "a@b.ru", "IT",
                null, PAST, false, EmployeeRole.EMPLOYEE));
        assertThrows(ValidationException.class, () -> new Employee(-5L, "Иванов Иван Иванович", "a@b.ru", "IT",
                null, PAST, false, EmployeeRole.EMPLOYEE));
    }
}
