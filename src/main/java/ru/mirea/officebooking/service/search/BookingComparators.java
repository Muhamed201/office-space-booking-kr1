package ru.mirea.officebooking.service.search;

import java.util.Comparator;
import java.util.Map;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Workspace;

public final class BookingComparators {

    private BookingComparators() {
    }

    public static Comparator<Booking> byStartTime() {
        return Comparator.comparing(Booking::getStartTime).thenComparing(Booking::getId);
    }

    public static Comparator<Booking> byDuration() {
        return Comparator.comparing(Booking::duration).thenComparing(Booking::getId);
    }

    public static Comparator<Booking> byEmployeeName(Map<Long, Employee> employeesById) {
        return Comparator.comparing(
                        (Booking b) -> nameOf(employeesById, b.getEmployeeId()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Booking::getId);
    }

    public static Comparator<Booking> byWorkspaceCode(Map<Long, Workspace> workspacesById) {
        return Comparator.comparing((Booking b) -> codeOf(workspacesById, b.getWorkspaceId()))
                .thenComparing(Booking::getId);
    }

    private static String nameOf(Map<Long, Employee> employeesById, Long id) {
        Employee employee = employeesById.get(id);
        return employee == null ? "" : employee.getFullName();
    }

    private static String codeOf(Map<Long, Workspace> workspacesById, Long id) {
        Workspace workspace = workspacesById.get(id);
        return workspace == null ? "" : workspace.getCode();
    }
}
