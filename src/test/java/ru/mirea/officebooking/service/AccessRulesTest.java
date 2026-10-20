package ru.mirea.officebooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mirea.officebooking.exception.AccessDeniedException;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.repository.EmployeeRepository;

class AccessRulesTest {

    private static final long EMPLOYEE = 1L;
    private static final long MANAGER = 2L;
    private static final long BLOCKED = 3L;

    private EmployeeService employeeService;
    private WorkspaceService workspaceService;
    private AccessPolicy policy;

    @BeforeEach
    void setUp() {
        Map<Long, Employee> data = new HashMap<>();
        data.put(EMPLOYEE, employee(EMPLOYEE, "Иванов Иван Иванович", "ivanov@company.ru", false, EmployeeRole.EMPLOYEE));
        data.put(MANAGER, employee(MANAGER, "Орлова Анна Викторовна", "orlova@company.ru", false,
                EmployeeRole.OFFICE_MANAGER));
        data.put(BLOCKED, employee(BLOCKED, "Петрова Мария Сергеевна", "petrova@company.ru", true,
                EmployeeRole.OFFICE_MANAGER));
        FakeEmployees employees = new FakeEmployees(data);
        employeeService = new EmployeeService(employees, null);
        workspaceService = new WorkspaceService(null, null, employees);
        policy = new AccessPolicy(employees);
    }

    private static Employee employee(long id, String name, String email, boolean blocked, EmployeeRole role) {
        return new Employee(id, name, email, "Отдел", null, LocalDate.of(2022, 1, 1), blocked, role);
    }

    @Test
    void loginFindsEmployeeByEmailIgnoringCaseAndSpaces() {
        assertEquals(EMPLOYEE, employeeService.login("  IVANOV@company.ru ").getId());
    }

    @Test
    void loginRejectsUnknownEmail() {
        assertThrows(AccessDeniedException.class, () -> employeeService.login("nobody@company.ru"));
    }

    @Test
    void loginRejectsBlockedEmployee() {
        assertThrows(AccessDeniedException.class, () -> employeeService.login("petrova@company.ru"));
    }

    @Test
    void loginRejectsMalformedEmail() {
        assertThrows(ValidationException.class, () -> employeeService.login("не почта"));
    }

    @Test
    void policyRecognisesRoles() {
        assertTrue(AccessPolicy.isManager(policy.requireActor(MANAGER)));
        assertFalse(AccessPolicy.isManager(policy.requireActor(EMPLOYEE)));
        assertThrows(AccessDeniedException.class, () -> policy.requireActor(BLOCKED));
        assertThrows(AccessDeniedException.class, () -> policy.requireManager(EMPLOYEE, "делать это"));
        assertEquals(EMPLOYEE, policy.requireOwnerOrManager(EMPLOYEE, EMPLOYEE, "изменить").getId());
        assertEquals(MANAGER, policy.requireOwnerOrManager(MANAGER, EMPLOYEE, "изменить").getId());
        assertThrows(AccessDeniedException.class, () -> policy.requireOwnerOrManager(EMPLOYEE, MANAGER, "изменить"));
    }

    @Test
    void employeeCannotManageEmployees() {
        assertThrows(AccessDeniedException.class, () -> employeeService.createAs(EMPLOYEE, "Новиков Павел Олегович",
                "novikov@company.ru", "IT", null, LocalDate.of(2023, 1, 1), EmployeeRole.EMPLOYEE));
        assertThrows(AccessDeniedException.class, () -> employeeService.updateAs(EMPLOYEE, EMPLOYEE,
                "Иванов Иван Иванович", "ivanov@company.ru", "IT", null, LocalDate.of(2022, 1, 1),
                EmployeeRole.OFFICE_MANAGER));
        assertThrows(AccessDeniedException.class, () -> employeeService.setBlockedAs(EMPLOYEE, MANAGER, true));
        assertThrows(AccessDeniedException.class, () -> employeeService.deleteAs(EMPLOYEE, MANAGER));
    }

    @Test
    void managerCannotBlockOrDeleteHimself() {
        assertThrows(ValidationException.class, () -> employeeService.setBlockedAs(MANAGER, MANAGER, true));
        assertThrows(ValidationException.class, () -> employeeService.deleteAs(MANAGER, MANAGER));
    }

    @Test
    void employeeCannotManageWorkspaces() {
        assertThrows(AccessDeniedException.class,
                () -> workspaceService.createAs(EMPLOYEE, WorkspaceType.OPEN_DESK, "A-99", 2, false, null));
        assertThrows(AccessDeniedException.class, () -> workspaceService.updateAs(EMPLOYEE, 1L, "A-99", 2, false));
        assertThrows(AccessDeniedException.class,
                () -> workspaceService.changeStatusAs(EMPLOYEE, 1L, WorkspaceStatus.MAINTENANCE));
        assertThrows(AccessDeniedException.class, () -> workspaceService.deleteAs(EMPLOYEE, 1L));
    }

    private static final class FakeEmployees extends EmployeeRepository {
        private final Map<Long, Employee> data;

        FakeEmployees(Map<Long, Employee> data) {
            super(null);
            this.data = data;
        }

        @Override
        public Optional<Employee> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Optional<Employee> findByEmail(String email) {
            return data.values().stream().filter(e -> e.getEmail().equals(email)).findFirst();
        }

        @Override
        public List<Employee> findAll() {
            return List.copyOf(data.values());
        }
    }
}
