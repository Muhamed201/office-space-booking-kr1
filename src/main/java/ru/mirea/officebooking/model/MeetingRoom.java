package ru.mirea.officebooking.model;

import ru.mirea.officebooking.exception.ValidationException;

public class MeetingRoom extends Workspace {

    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 100;

    private static final int MAX_BOOKING_HOURS = 4;

    private final int capacity;

    public MeetingRoom(Long id, String code, int floor, WorkspaceStatus status, boolean hasMonitor, int capacity) {
        super(id, code, floor, status, hasMonitor);
        this.capacity = validateCapacity(capacity);
    }

    public static int validateCapacity(int value) {
        return Validation.requireInRange(value, MIN_CAPACITY, MAX_CAPACITY, "Вместимость переговорной");
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public WorkspaceType type() {
        return WorkspaceType.MEETING_ROOM;
    }

    @Override
    public int maxBookingHours() {
        return MAX_BOOKING_HOURS;
    }

    @Override
    public void validateBooking(Booking booking) {
        Integer attendees = booking.getAttendees();
        if (attendees == null) {
            throw new ValidationException("Для переговорной " + getCode() + " необходимо указать число участников");
        }
        if (attendees < 1 || attendees > capacity) {
            throw new ValidationException(
                    "Число участников должно быть от 1 до %d (вместимость переговорной %s), получено: %d"
                            .formatted(capacity, getCode(), attendees));
        }
    }

    @Override
    public Workspace withId(long newId) {
        return new MeetingRoom(newId, getCode(), getFloor(), getStatus(), isHasMonitor(), capacity);
    }

    @Override
    public Workspace withStatus(WorkspaceStatus newStatus) {
        return new MeetingRoom(getId(), getCode(), getFloor(), newStatus, isHasMonitor(), capacity);
    }

    @Override
    public Workspace withDetails(String newCode, int newFloor, boolean newHasMonitor) {
        return new MeetingRoom(getId(), newCode, newFloor, getStatus(), newHasMonitor, capacity);
    }

    @Override
    public String toString() {
        return super.toString() + ", вместимость: " + capacity;
    }
}
