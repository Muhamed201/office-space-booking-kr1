package ru.mirea.officebooking.model;

import java.util.Locale;
import java.util.regex.Pattern;
import ru.mirea.officebooking.exception.ValidationException;

public abstract class Workspace {

    public static final int MAX_CODE_LENGTH = 20;
    public static final int MIN_FLOOR = -2;
    public static final int MAX_FLOOR = 50;

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]{1,3}-\\d{1,3}$");

    private final Long id;
    private final String code;
    private final int floor;
    private final WorkspaceStatus status;
    private final boolean hasMonitor;

    protected Workspace(Long id, String code, int floor, WorkspaceStatus status, boolean hasMonitor) {
        this.id = Validation.optionalPositiveId(id, "id рабочего места");
        this.code = normalizeCode(code);
        this.floor = validateFloor(floor);
        this.status = Validation.requireNotNull(status, "статус рабочего места");
        this.hasMonitor = hasMonitor;
    }

    public static String normalizeCode(String value) {
        String code = Validation.requireText(value, "Код места", MAX_CODE_LENGTH).toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new ValidationException(
                    "Код места должен быть в формате «A-14»: 1–3 латинские буквы, дефис, 1–3 цифры, получено: " + code);
        }
        return code;
    }

    public static int validateFloor(int value) {
        return Validation.requireInRange(value, MIN_FLOOR, MAX_FLOOR, "Этаж");
    }

    public abstract WorkspaceType type();

    public abstract int maxBookingHours();

    public abstract void validateBooking(Booking booking);

    public abstract Workspace withId(long newId);

    public abstract Workspace withStatus(WorkspaceStatus newStatus);

    public abstract Workspace withDetails(String newCode, int newFloor, boolean newHasMonitor);

    public int getCapacity() {
        return 1;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public int getFloor() {
        return floor;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public boolean isHasMonitor() {
        return hasMonitor;
    }

    public boolean isAvailable() {
        return status == WorkspaceStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "#%d %s [%s, этаж %d, %s, монитор: %s]"
                .formatted(id, code, type(), floor, status, hasMonitor ? "есть" : "нет");
    }
}
