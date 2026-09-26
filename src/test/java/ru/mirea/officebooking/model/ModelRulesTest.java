package ru.mirea.officebooking.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import ru.mirea.officebooking.exception.ValidationException;

class ModelRulesTest {

    private static final LocalDateTime T10 = LocalDateTime.of(2026, 9, 25, 10, 0);
    private static final LocalDateTime T12 = LocalDateTime.of(2026, 9, 25, 12, 0);
    private static final LocalDateTime T14 = LocalDateTime.of(2026, 9, 25, 14, 0);

    private static Booking booking(LocalDateTime start, LocalDateTime end, Integer attendees) {
        return new Booking(1L, 1L, 1L, start, end, BookingStatus.ACTIVE, attendees, null, null);
    }

    @Test
    void activeCanGoToAnyFinalStatus() {
        assertTrue(BookingStatus.ACTIVE.canTransitionTo(BookingStatus.CANCELLED));
        assertTrue(BookingStatus.ACTIVE.canTransitionTo(BookingStatus.COMPLETED));
        assertTrue(BookingStatus.ACTIVE.canTransitionTo(BookingStatus.NO_SHOW));
        assertFalse(BookingStatus.ACTIVE.canTransitionTo(BookingStatus.ACTIVE));
        assertFalse(BookingStatus.ACTIVE.isFinal());
    }

    @Test
    void finalStatusesHaveNoTransitions() {
        for (BookingStatus finalStatus : new BookingStatus[]{
                BookingStatus.CANCELLED, BookingStatus.COMPLETED, BookingStatus.NO_SHOW}) {
            assertTrue(finalStatus.isFinal());
            for (BookingStatus target : BookingStatus.values()) {
                assertFalse(finalStatus.canTransitionTo(target));
            }
        }
    }

    @Test
    void adjacentIntervalsDoNotOverlap() {
        assertFalse(booking(T10, T12, null).overlaps(booking(T12, T14, null)));
        assertFalse(booking(T12, T14, null).overlaps(booking(T10, T12, null)));
    }

    @Test
    void crossingIntervalsOverlap() {
        assertTrue(booking(T10, T14, null).overlaps(booking(T12, T14, null)));
        assertTrue(booking(T10, T12, null).overlaps(booking(T10, T12, null)));
    }

    @Test
    void bookingRequiresStartBeforeEnd() {
        assertThrows(ValidationException.class, () -> booking(T12, T10, null));
        assertThrows(ValidationException.class, () -> booking(T10, T10, null));
        assertThrows(ValidationException.class, () -> booking(null, T10, null));
    }

    @Test
    void bookingValidatesIdsAttendeesAndPurpose() {
        assertThrows(ValidationException.class,
                () -> new Booking(0L, 1L, T10, T12, 1, null));
        assertThrows(ValidationException.class,
                () -> new Booking(1L, -1L, T10, T12, 1, null));
        assertThrows(ValidationException.class,
                () -> new Booking(1L, 1L, T10, T12, 0, null));
        assertThrows(ValidationException.class,
                () -> new Booking(1L, 1L, T10, T12, null, "я".repeat(Booking.MAX_PURPOSE_LENGTH + 1)));
    }

    @Test
    void durationIsMeasuredInHours() {
        assertEquals(4, booking(T10, T14, null).durationHours());
    }

    @Test
    void workspaceCodeIsNormalizedAndValidated() {
        assertEquals("A-14", Workspace.normalizeCode(" a-14 "));
        for (String bad : new String[]{"A14", "AAAA-1", "A-1234", "1-A", "", "  ", "А-01", "A-"}) {
            assertThrows(ValidationException.class, () -> Workspace.normalizeCode(bad), bad);
        }
    }

    @Test
    void workspaceFloorIsRangeChecked() {
        assertDoesNotThrow(() -> Workspace.validateFloor(-2));
        assertDoesNotThrow(() -> Workspace.validateFloor(50));
        assertThrows(ValidationException.class, () -> Workspace.validateFloor(-3));
        assertThrows(ValidationException.class, () -> Workspace.validateFloor(51));
    }

    @Test
    void maxBookingHoursDependOnWorkspaceType() {
        assertEquals(10, new OpenDesk(null, "A-01", 1, WorkspaceStatus.AVAILABLE, true).maxBookingHours());
        assertEquals(1, new PhoneBooth(null, "B-01", 1, WorkspaceStatus.AVAILABLE, false).maxBookingHours());
        assertEquals(4, new MeetingRoom(null, "M-01", 1, WorkspaceStatus.AVAILABLE, true, 6).maxBookingHours());
    }

    @Test
    void meetingRoomChecksAttendees() {
        MeetingRoom room = new MeetingRoom(1L, "M-01", 3, WorkspaceStatus.AVAILABLE, true, 4);
        assertDoesNotThrow(() -> room.validateBooking(booking(T10, T12, 4)));
        assertThrows(ValidationException.class, () -> room.validateBooking(booking(T10, T12, null)));
        assertThrows(ValidationException.class, () -> room.validateBooking(booking(T10, T12, 5)));
    }

    @Test
    void deskAndBoothRejectAttendees() {
        OpenDesk desk = new OpenDesk(1L, "A-01", 2, WorkspaceStatus.AVAILABLE, true);
        PhoneBooth booth = new PhoneBooth(2L, "B-01", 2, WorkspaceStatus.AVAILABLE, false);
        assertDoesNotThrow(() -> desk.validateBooking(booking(T10, T12, null)));
        assertThrows(ValidationException.class, () -> desk.validateBooking(booking(T10, T12, 2)));
        assertThrows(ValidationException.class, () -> booth.validateBooking(booking(T10, T11(), 2)));
    }

    @Test
    void meetingRoomCapacityIsRangeChecked() {
        assertThrows(ValidationException.class,
                () -> new MeetingRoom(null, "M-01", 1, WorkspaceStatus.AVAILABLE, true, 0));
        assertThrows(ValidationException.class,
                () -> new MeetingRoom(null, "M-01", 1, WorkspaceStatus.AVAILABLE, true, MeetingRoom.MAX_CAPACITY + 1));
    }

    private static LocalDateTime T11() {
        return LocalDateTime.of(2026, 9, 25, 11, 0);
    }
}
