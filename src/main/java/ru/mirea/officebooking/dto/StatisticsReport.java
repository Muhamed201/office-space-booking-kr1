package ru.mirea.officebooking.dto;

import java.util.List;
import java.util.Map;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.WorkspaceType;

public record StatisticsReport(
        long totalBookings,
        long bookingsInPeriod,
        Map<BookingStatus, Long> byStatus,
        Map<WorkspaceType, Long> byWorkspaceType,
        double averageDurationHours,
        double minDurationHours,
        double maxDurationHours,
        double noShowPercent,
        List<NamedCount> topWorkspaces,
        List<NamedCount> topEmployees,
        long totalEmployees,
        long blockedEmployees) {

    public record NamedCount(String label, long count) {
    }
}
