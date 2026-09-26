package ru.mirea.officebooking.ui;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.service.EmployeeService;
import ru.mirea.officebooking.service.WorkspaceService;
import ru.mirea.officebooking.service.search.BookingComparators;

public class BookingPrinter {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EmployeeService employeeService;
    private final WorkspaceService workspaceService;
    private final ConsoleReader reader;

    public BookingPrinter(EmployeeService employeeService, WorkspaceService workspaceService, ConsoleReader reader) {
        this.employeeService = employeeService;
        this.workspaceService = workspaceService;
        this.reader = reader;
    }

    public void printSorted(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            System.out.println("Ничего не найдено.");
            return;
        }

        Map<Long, Employee> employeesById = employeeService.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        Map<Long, Workspace> workspacesById = workspaceService.findAll().stream()
                .collect(Collectors.toMap(Workspace::getId, w -> w));

        System.out.println("Найдено броней: " + bookings.size());
        System.out.println("Сортировать по: 1) началу  2) сотруднику  3) коду места  4) длительности  0) без сортировки");
        int choice = reader.readIntInRange("Выбор: ", 0, 4);
        Comparator<Booking> comparator = switch (choice) {
            case 1 -> BookingComparators.byStartTime();
            case 2 -> BookingComparators.byEmployeeName(employeesById);
            case 3 -> BookingComparators.byWorkspaceCode(workspacesById);
            case 4 -> BookingComparators.byDuration();
            default -> null;
        };

        List<Booking> toPrint = bookings;
        if (comparator != null) {
            if (reader.readYesNo("По убыванию?")) {
                comparator = comparator.reversed();
            }
            toPrint = bookings.stream().sorted(comparator).toList();
        }

        for (Booking b : toPrint) {
            Employee employee = employeesById.get(b.getEmployeeId());
            Workspace workspace = workspacesById.get(b.getWorkspaceId());
            System.out.printf("#%-4d %-32s %-7s %s — %s  %-9s %s%n",
                    b.getId(),
                    employee != null ? employee.getFullName() : "id=" + b.getEmployeeId(),
                    workspace != null ? workspace.getCode() : "id=" + b.getWorkspaceId(),
                    b.getStartTime().format(DATE_TIME_FORMAT),
                    b.getEndTime().format(DATE_TIME_FORMAT),
                    b.getStatus(),
                    b.getPurpose() == null ? "" : b.getPurpose());
        }
    }
}
