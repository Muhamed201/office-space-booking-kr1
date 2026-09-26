package ru.mirea.officebooking.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mirea.officebooking.exception.BookingConflictException;
import ru.mirea.officebooking.exception.EntityNotFoundException;
import ru.mirea.officebooking.exception.InvalidStatusTransitionException;
import ru.mirea.officebooking.exception.ValidationException;
import ru.mirea.officebooking.exception.WorkspaceUnavailableException;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.model.MeetingRoom;
import ru.mirea.officebooking.model.OpenDesk;
import ru.mirea.officebooking.model.PhoneBooth;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.repository.BookingRepository;
import ru.mirea.officebooking.repository.EmployeeRepository;
import ru.mirea.officebooking.repository.WorkspaceRepository;

class BookingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 0);
    private static final LocalDateTime TOMORROW_9 = LocalDateTime.of(2026, 9, 25, 9, 0);

    private FakeBookings bookings;
    private BookingService service;

    @BeforeEach
    void setUp() {
        Map<Long, Employee> employees = new HashMap<>();
        employees.put(1L, employee(1L, "Иванов Иван Иванович", false));
        employees.put(2L, employee(2L, "Петрова Мария Сергеевна", false));
        employees.put(3L, employee(3L, "Сидоров Алексей Викторович", true));

        Map<Long, Workspace> workspaces = new HashMap<>();
        workspaces.put(1L, new OpenDesk(1L, "A-01", 2, WorkspaceStatus.AVAILABLE, true));
        workspaces.put(2L, new OpenDesk(2L, "A-02", 2, WorkspaceStatus.MAINTENANCE, true));
        workspaces.put(3L, new PhoneBooth(3L, "B-01", 2, WorkspaceStatus.AVAILABLE, false));
        workspaces.put(4L, new MeetingRoom(4L, "M-01", 3, WorkspaceStatus.AVAILABLE, true, 4));
        workspaces.put(5L, new OpenDesk(5L, "A-03", 2, WorkspaceStatus.AVAILABLE, false));

        bookings = new FakeBookings();
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new BookingService(bookings, new FakeEmployees(employees), new FakeWorkspaces(workspaces), clock);
    }

    private static Employee employee(long id, String name, boolean blocked) {
        return new Employee(id, name, "user" + id + "@company.ru", "IT", null,
                LocalDate.of(2022, 1, 1), blocked, EmployeeRole.EMPLOYEE);
    }

    private Booking createDesk(long employeeId, int startHour, int endHour) {
        return service.create(employeeId, 1L, TOMORROW_9.withHour(startHour), TOMORROW_9.withHour(endHour), null, null);
    }

    @Test
    void createsValidBooking() {
        Booking created = createDesk(1L, 9, 17);
        assertEquals(1L, created.getId());
        assertEquals(BookingStatus.ACTIVE, created.getStatus());
    }

    @Test
    void unknownEmployeeOrWorkspaceIsNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> service.create(99L, 1L, TOMORROW_9, TOMORROW_9.plusHours(1), null, null));
        assertThrows(EntityNotFoundException.class,
                () -> service.create(1L, 99L, TOMORROW_9, TOMORROW_9.plusHours(1), null, null));
    }

    @Test
    void nonPositiveIdsAndNullTimesAreRejected() {
        assertThrows(ValidationException.class,
                () -> service.create(0L, 1L, TOMORROW_9, TOMORROW_9.plusHours(1), null, null));
        assertThrows(ValidationException.class,
                () -> service.create(1L, -1L, TOMORROW_9, TOMORROW_9.plusHours(1), null, null));
        assertThrows(ValidationException.class, () -> service.create(1L, 1L, null, TOMORROW_9, null, null));
        assertThrows(ValidationException.class, () -> service.create(1L, 1L, TOMORROW_9, null, null, null));
    }

    @Test
    void blockedEmployeeCannotBook() {
        assertThrows(ValidationException.class, () -> service.create(3L, 1L, TOMORROW_9, TOMORROW_9.plusHours(1), null, null));
    }

    @Test
    void unavailableWorkspaceCannotBeBooked() {
        assertThrows(WorkspaceUnavailableException.class,
                () -> service.create(1L, 2L, TOMORROW_9, TOMORROW_9.plusHours(1), null, null));
    }

    @Test
    void startInThePastIsRejected() {
        LocalDateTime past = NOW.minusDays(1).withHour(9);
        assertThrows(ValidationException.class, () -> service.create(1L, 1L, past, past.plusHours(1), null, null));
    }

    @Test
    void startBeyondHorizonIsRejected() {
        LocalDateTime far = NOW.plusDays(BookingService.BOOKING_HORIZON_DAYS + 1).withHour(9);
        assertThrows(ValidationException.class, () -> service.create(1L, 1L, far, far.plusHours(1), null, null));
        LocalDateTime edge = NOW.plusDays(BookingService.BOOKING_HORIZON_DAYS).withHour(9);
        assertDoesNotThrow(() -> service.create(1L, 1L, edge, edge.plusHours(1), null, null));
    }

    @Test
    void timeMustBeWholeHours() {
        assertThrows(ValidationException.class,
                () -> service.create(1L, 1L, TOMORROW_9.withMinute(30), TOMORROW_9.plusHours(2), null, null));
        assertThrows(ValidationException.class,
                () -> service.create(1L, 1L, TOMORROW_9, TOMORROW_9.plusHours(1).withMinute(15), null, null));
    }

    @Test
    void endMustBeAfterStart() {
        assertThrows(ValidationException.class,
                () -> service.create(1L, 1L, TOMORROW_9.plusHours(2), TOMORROW_9, null, null));
        assertThrows(ValidationException.class, () -> service.create(1L, 1L, TOMORROW_9, TOMORROW_9, null, null));
    }

    @Test
    void bookingMustFitWorkday() {
        assertThrows(ValidationException.class, () -> createDesk(1L, 7, 9));
        assertThrows(ValidationException.class, () -> createDesk(1L, 21, 23));
        assertDoesNotThrow(() -> createDesk(1L, 8, 10));
        assertDoesNotThrow(() -> createDesk(2L, 20, 22));
    }

    @Test
    void bookingMustNotSpanTwoDays() {
        assertThrows(ValidationException.class,
                () -> service.create(1L, 1L, TOMORROW_9.withHour(21), TOMORROW_9.plusDays(1).withHour(9), null, null));
    }

    @Test
    void durationLimitDependsOnWorkspaceType() {
        assertDoesNotThrow(() -> createDesk(1L, 8, 18));
        assertThrows(ValidationException.class, () -> createDesk(2L, 8, 19));
        assertThrows(ValidationException.class,
                () -> service.create(2L, 3L, TOMORROW_9, TOMORROW_9.plusHours(2), null, null));
        assertDoesNotThrow(() -> service.create(2L, 3L, TOMORROW_9, TOMORROW_9.plusHours(1), null, null));
        assertThrows(ValidationException.class,
                () -> service.create(1L, 4L, TOMORROW_9, TOMORROW_9.plusHours(5), 2, null));
    }

    @Test
    void workspaceDoubleBookingIsRejectedButAdjacentIsAllowed() {
        createDesk(1L, 10, 12);
        assertThrows(BookingConflictException.class, () -> createDesk(2L, 11, 13));
        assertThrows(BookingConflictException.class, () -> createDesk(2L, 10, 12));
        assertDoesNotThrow(() -> createDesk(2L, 12, 14));
        assertDoesNotThrow(() -> createDesk(2L, 8, 10).getId());
    }

    @Test
    void employeeCannotBeInTwoPlacesAtOnce() {
        createDesk(1L, 10, 12);
        assertThrows(BookingConflictException.class,
                () -> service.create(1L, 5L, TOMORROW_9.withHour(11), TOMORROW_9.withHour(13), null, null));
        assertDoesNotThrow(
                () -> service.create(1L, 5L, TOMORROW_9.withHour(12), TOMORROW_9.withHour(13), null, null));
    }

    @Test
    void cancelledBookingDoesNotBlockTheSlot() {
        Booking first = createDesk(1L, 10, 12);
        service.cancel(first.getId());
        assertDoesNotThrow(() -> createDesk(2L, 10, 12));
    }

    @Test
    void meetingRoomNeedsValidAttendees() {
        LocalDateTime end = TOMORROW_9.plusHours(2);
        assertThrows(ValidationException.class, () -> service.create(1L, 4L, TOMORROW_9, end, null, null));
        assertThrows(ValidationException.class, () -> service.create(1L, 4L, TOMORROW_9, end, 5, null));
        assertThrows(ValidationException.class, () -> service.create(1L, 4L, TOMORROW_9, end, 0, null));
        assertDoesNotThrow(() -> service.create(1L, 4L, TOMORROW_9, end, 4, "Планёрка"));
    }

    @Test
    void deskRejectsAttendees() {
        assertThrows(ValidationException.class,
                () -> service.create(1L, 1L, TOMORROW_9, TOMORROW_9.plusHours(1), 2, null));
    }

    @Test
    void tooLongPurposeIsRejected() {
        assertThrows(ValidationException.class, () -> service.create(1L, 1L, TOMORROW_9, TOMORROW_9.plusHours(1), null,
                "я".repeat(Booking.MAX_PURPOSE_LENGTH + 1)));
    }

    @Test
    void futureBookingCanBeCancelled() {
        Booking created = createDesk(1L, 10, 12);
        assertEquals(BookingStatus.CANCELLED, service.cancel(created.getId()).getStatus());
    }

    @Test
    void startedBookingCannotBeCancelledButCanBeMarkedNoShow() {
        Booking started = bookings.seed(1L, 1L, NOW.minusHours(1), NOW.plusHours(3));
        assertThrows(InvalidStatusTransitionException.class, () -> service.cancel(started.getId()));
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.changeStatus(started.getId(), BookingStatus.COMPLETED));
        assertEquals(BookingStatus.NO_SHOW, service.changeStatus(started.getId(), BookingStatus.NO_SHOW).getStatus());
    }

    @Test
    void finishedBookingCanBeCompletedButNotNoShowBeforeStart() {
        Booking finished = bookings.seed(1L, 1L, NOW.minusHours(5), NOW.minusHours(1));
        assertEquals(BookingStatus.COMPLETED, service.changeStatus(finished.getId(), BookingStatus.COMPLETED).getStatus());

        Booking future = createDesk(2L, 10, 12);
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.changeStatus(future.getId(), BookingStatus.NO_SHOW));
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.changeStatus(future.getId(), BookingStatus.COMPLETED));
    }

    @Test
    void finalStatusCannotBeChanged() {
        Booking created = createDesk(1L, 10, 12);
        service.cancel(created.getId());
        for (BookingStatus target : BookingStatus.values()) {
            assertThrows(InvalidStatusTransitionException.class, () -> service.changeStatus(created.getId(), target));
        }
    }

    @Test
    void statusChangeValidatesArguments() {
        assertThrows(ValidationException.class, () -> service.changeStatus(1L, null));
        assertThrows(EntityNotFoundException.class, () -> service.changeStatus(99L, BookingStatus.CANCELLED));
        assertThrows(ValidationException.class, () -> service.changeStatus(0L, BookingStatus.CANCELLED));
    }

    @Test
    void updateCanMoveBookingWithoutConflictingWithItself() {
        Booking created = createDesk(1L, 10, 12);
        Booking moved = service.updateTime(created.getId(), TOMORROW_9.withHour(11), TOMORROW_9.withHour(13), null, "Сдвинул");
        assertEquals(TOMORROW_9.withHour(11), moved.getStartTime());
        assertEquals("Сдвинул", bookings.findById(created.getId()).orElseThrow().getPurpose());
    }

    @Test
    void updateIsCheckedAgainstOtherBookings() {
        Booking first = createDesk(1L, 10, 12);
        createDesk(2L, 13, 15);
        assertThrows(BookingConflictException.class,
                () -> service.updateTime(first.getId(), TOMORROW_9.withHour(12), TOMORROW_9.withHour(14), null, null));
    }

    @Test
    void updateRejectsInactiveOrStartedBookings() {
        Booking created = createDesk(1L, 10, 12);
        service.cancel(created.getId());
        assertThrows(ValidationException.class,
                () -> service.updateTime(created.getId(), TOMORROW_9, TOMORROW_9.plusHours(1), null, null));

        Booking started = bookings.seed(2L, 5L, NOW.minusHours(1), NOW.plusHours(2));
        assertThrows(ValidationException.class,
                () -> service.updateTime(started.getId(), TOMORROW_9, TOMORROW_9.plusHours(1), null, null));
    }

    @Test
    void updateAppliesIntervalRulesToo() {
        Booking created = createDesk(1L, 10, 12);
        assertThrows(ValidationException.class,
                () -> service.updateTime(created.getId(), TOMORROW_9.withHour(7), TOMORROW_9.withHour(9), null, null));
        assertThrows(ValidationException.class,
                () -> service.updateTime(created.getId(), TOMORROW_9.withHour(8), TOMORROW_9.withHour(20), null, null));
    }

    @Test
    void deleteRemovesBookingAndRejectsUnknownId() {
        Booking created = createDesk(1L, 10, 12);
        service.delete(created.getId());
        assertThrows(EntityNotFoundException.class, () -> service.findById(created.getId()));
        assertThrows(EntityNotFoundException.class, () -> service.delete(created.getId()));
    }

    private static final class FakeEmployees extends EmployeeRepository {
        private final Map<Long, Employee> data;

        FakeEmployees(Map<Long, Employee> data) {
            super(null);
            this.data = data;
        }

        @Override
        public Optional<Employee> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }
    }

    private static final class FakeWorkspaces extends WorkspaceRepository {
        private final Map<Long, Workspace> data;

        FakeWorkspaces(Map<Long, Workspace> data) {
            super(null);
            this.data = data;
        }

        @Override
        public Optional<Workspace> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }
    }

    private static final class FakeBookings extends BookingRepository {
        private final List<Booking> data = new ArrayList<>();
        private long nextId = 1;

        FakeBookings() {
            super(null);
        }

        Booking seed(long employeeId, long workspaceId, LocalDateTime start, LocalDateTime end) {
            return save(new Booking(employeeId, workspaceId, start, end, null, null));
        }

        @Override
        public Booking save(Booking booking) {
            Booking saved = booking.withId(nextId++);
            data.add(saved);
            return saved;
        }

        @Override
        public Optional<Booking> findById(Long id) {
            return data.stream().filter(b -> b.getId().equals(id)).findFirst();
        }

        @Override
        public List<Booking> findAll() {
            return List.copyOf(data);
        }

        @Override
        public void update(Booking booking) {
            data.removeIf(b -> b.getId().equals(booking.getId()));
            data.add(booking);
        }

        @Override
        public void deleteById(Long id) {
            data.removeIf(b -> b.getId().equals(id));
        }

        @Override
        public List<Booking> findActiveOverlappingForWorkspace(long workspaceId, LocalDateTime start,
                                                               LocalDateTime end, Long excludeBookingId) {
            return data.stream()
                    .filter(b -> b.getWorkspaceId() == workspaceId && overlaps(b, start, end, excludeBookingId))
                    .toList();
        }

        @Override
        public List<Booking> findActiveOverlappingForEmployee(long employeeId, LocalDateTime start,
                                                              LocalDateTime end, Long excludeBookingId) {
            return data.stream()
                    .filter(b -> b.getEmployeeId() == employeeId && overlaps(b, start, end, excludeBookingId))
                    .toList();
        }

        private static boolean overlaps(Booking b, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
            return b.isActive()
                    && (excludeBookingId == null || !b.getId().equals(excludeBookingId))
                    && b.getStartTime().isBefore(end)
                    && start.isBefore(b.getEndTime());
        }
    }
}
