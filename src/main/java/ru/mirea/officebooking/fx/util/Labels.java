package ru.mirea.officebooking.fx.util;

import java.util.function.Function;
import javafx.util.StringConverter;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;

public final class Labels {

    private Labels() {
    }

    public static String of(BookingStatus status) {
        return switch (status) {
            case ACTIVE -> "Активна";
            case CANCELLED -> "Отменена";
            case COMPLETED -> "Завершена";
            case NO_SHOW -> "Неявка";
        };
    }

    public static String of(WorkspaceType type) {
        return switch (type) {
            case OPEN_DESK -> "Рабочее место";
            case MEETING_ROOM -> "Переговорная";
            case PHONE_BOOTH -> "Телефонная кабина";
        };
    }

    public static String of(WorkspaceStatus status) {
        return switch (status) {
            case AVAILABLE -> "Доступно";
            case MAINTENANCE -> "На обслуживании";
            case DECOMMISSIONED -> "Списано";
        };
    }

    public static String of(EmployeeRole role) {
        return switch (role) {
            case EMPLOYEE -> "Сотрудник";
            case OFFICE_MANAGER -> "Офис-менеджер";
        };
    }

    public static String of(Employee employee) {
        return "%s (%s)".formatted(employee.getFullName(), employee.getEmail());
    }

    public static String of(Workspace workspace) {
        String capacity = workspace.type() == WorkspaceType.MEETING_ROOM
                ? ", мест: " + workspace.getCapacity() : "";
        return "%s · этаж %d · %s%s".formatted(workspace.getCode(), workspace.getFloor(), of(workspace.type()), capacity);
    }

    public static <T> StringConverter<T> converter(Function<T, String> labeler) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : labeler.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        };
    }
}
