package ru.mirea.officebooking.dto;

import java.util.Map;

public class AnalyticsResponse {
    private long totalBookings;
    private long activeBookings;
    private long archivedBookings;
    private long recurringBookings;
    private double averageAttendees;
    private Map<String, Long> byWorkspace;
    private Map<String, Long> byStatus;
    private Map<Integer, Long> byPriority;

    public AnalyticsResponse() {}

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }
    public long getActiveBookings() { return activeBookings; }
    public void setActiveBookings(long activeBookings) { this.activeBookings = activeBookings; }
    public long getArchivedBookings() { return archivedBookings; }
    public void setArchivedBookings(long archivedBookings) { this.archivedBookings = archivedBookings; }
    public long getRecurringBookings() { return recurringBookings; }
    public void setRecurringBookings(long recurringBookings) { this.recurringBookings = recurringBookings; }
    public double getAverageAttendees() { return averageAttendees; }
    public void setAverageAttendees(double averageAttendees) { this.averageAttendees = averageAttendees; }
    public Map<String, Long> getByWorkspace() { return byWorkspace; }
    public void setByWorkspace(Map<String, Long> byWorkspace) { this.byWorkspace = byWorkspace; }
    public Map<String, Long> getByStatus() { return byStatus; }
    public void setByStatus(Map<String, Long> byStatus) { this.byStatus = byStatus; }
    public Map<Integer, Long> getByPriority() { return byPriority; }
    public void setByPriority(Map<Integer, Long> byPriority) { this.byPriority = byPriority; }
}
