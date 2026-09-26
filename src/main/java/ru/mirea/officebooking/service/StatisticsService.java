package ru.mirea.officebooking.service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Validation;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.repository.EmployeeRepository;
import ru.mirea.officebooking.repository.WorkspaceRepository;

public class StatisticsService {

    public record NamedCount(String label, long count) {
    }

    private final EmployeeRepository employeeRepository;
    private final WorkspaceRepository workspaceRepository;
    private final BookingRepository bookingRepository;

    public StatisticsService(EmployeeRepository employeeRepository, WorkspaceRepository workspaceRepository,
                             BookingRepository bookingRepository) {
        this.employeeRepository = employeeRepository;
        this.workspaceRepository = workspaceRepository;
        this.bookingRepository = bookingRepository;
    }

    public long totalEmployees() {
        return employeeRepository.findAll().size();
    }

    public long blockedEmployees() {
        return employeeRepository.findAll().stream().filter(Employee::isBlocked).count();
    }

    public Map<WorkspaceType, Long> workspacesByType() {
        Map<WorkspaceType, Long> counts = new EnumMap<>(WorkspaceType.class);
        for (WorkspaceType type : WorkspaceType.values()) {
            counts.put(type, 0L);
        }
        workspaceRepository.findAll().forEach(w -> counts.merge(w.type(), 1L, Long::sum));
        return counts;
    }

    public Map<WorkspaceStatus, Long> workspacesByStatus() {
        Map<WorkspaceStatus, Long> counts = new EnumMap<>(WorkspaceStatus.class);
        for (WorkspaceStatus status : WorkspaceStatus.values()) {
            counts.put(status, 0L);
        }
        workspaceRepository.findAll().forEach(w -> counts.merge(w.getStatus(), 1L, Long::sum));
        return counts;
    }

    public Map<BookingStatus, Long> bookingsByStatus() {
        Map<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            counts.put(status, 0L);
        }
        bookingRepository.findAll().forEach(b -> counts.merge(b.getStatus(), 1L, Long::sum));
        return counts;
    }

    public double averageDurationHoursCompleted() {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .mapToLong(b -> b.duration().toMinutes())
                .average()
                .orElse(0.0) / 60.0;
    }

    public double noShowRate() {
        Map<BookingStatus, Long> counts = bookingsByStatus();
        long denominator = counts.get(BookingStatus.COMPLETED) + counts.get(BookingStatus.NO_SHOW);
        return denominator == 0 ? 0.0 : (double) counts.get(BookingStatus.NO_SHOW) / denominator;
    }

    public List<NamedCount> topWorkspaces(int limit) {
        Validation.requireInRange(limit, 1, 100, "Размер топа");
        Map<Long, Workspace> workspacesById = workspaceRepository.findAll().stream()
                .collect(Collectors.toMap(Workspace::getId, w -> w));
        Map<Long, Long> counts = bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getWorkspaceId, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(e -> new NamedCount(
                        workspacesById.containsKey(e.getKey()) ? workspacesById.get(e.getKey()).getCode() : "id=" + e.getKey(),
                        e.getValue()))
                .toList();
    }

    public List<NamedCount> topEmployees(int limit) {
        Validation.requireInRange(limit, 1, 100, "Размер топа");
        Map<Long, Employee> employeesById = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        Map<Long, Long> counts = bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getEmployeeId, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(e -> new NamedCount(
                        employeesById.containsKey(e.getKey()) ? employeesById.get(e.getKey()).getFullName() : "id=" + e.getKey(),
                        e.getValue()))
                .toList();
    }

    public NamedCount busiestFloor() {
        Map<Long, Workspace> workspacesById = workspaceRepository.findAll().stream()
                .collect(Collectors.toMap(Workspace::getId, w -> w));
        Map<Integer, Long> byFloor = bookingRepository.findAll().stream()
                .map(b -> workspacesById.get(b.getWorkspaceId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Workspace::getFloor, Collectors.counting()));
        return byFloor.entrySet().stream()
                .max(Comparator.comparingLong((Map.Entry<Integer, Long> e) -> e.getValue()).thenComparing(Map.Entry::getKey))
                .map(e -> new NamedCount("этаж " + e.getKey(), e.getValue()))
                .orElse(new NamedCount("нет данных", 0));
    }
}
