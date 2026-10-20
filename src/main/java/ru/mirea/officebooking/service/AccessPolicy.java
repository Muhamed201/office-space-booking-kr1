package ru.mirea.officebooking.service;

import ru.mirea.officebooking.exception.AccessDeniedException;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.model.Validation;
import ru.mirea.officebooking.repository.EmployeeRepository;

public class AccessPolicy {

    private final EmployeeRepository employeeRepository;

    public AccessPolicy(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public static boolean isManager(Employee employee) {
        return employee.getRole() == EmployeeRole.OFFICE_MANAGER;
    }

    public Employee requireActor(long actorId) {
        Validation.requirePositiveId(actorId, "id пользователя");
        Employee actor = employeeRepository.findById(actorId)
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден, выполните вход заново"));
        if (actor.isBlocked()) {
            throw new AccessDeniedException("Пользователь %s заблокирован, действия запрещены".formatted(actor.getFullName()));
        }
        return actor;
    }

    public Employee requireManager(long actorId, String action) {
        Employee actor = requireActor(actorId);
        if (!isManager(actor)) {
            throw new AccessDeniedException("Недостаточно прав: %s может только офис-менеджер".formatted(action));
        }
        return actor;
    }

    public Employee requireOwnerOrManager(long actorId, long ownerId, String action) {
        Employee actor = requireActor(actorId);
        if (actor.getId() != ownerId && !isManager(actor)) {
            throw new AccessDeniedException(
                    "Недостаточно прав: %s чужую бронь может только офис-менеджер".formatted(action));
        }
        return actor;
    }
}
