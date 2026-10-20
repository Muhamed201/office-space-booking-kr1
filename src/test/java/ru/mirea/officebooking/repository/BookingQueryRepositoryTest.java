package ru.mirea.officebooking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.mirea.officebooking.dto.BookingFilter;
import ru.mirea.officebooking.dto.BookingRow;
import ru.mirea.officebooking.dto.BookingSort;
import ru.mirea.officebooking.dto.StatisticsReport;
import ru.mirea.officebooking.exception.BookingConflictException;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.model.MeetingRoom;
import ru.mirea.officebooking.model.OpenDesk;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.service.StatisticsService;
import ru.mirea.officebooking.util.DatabaseManager;
import ru.mirea.officebooking.util.DatabaseMigrator;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
class BookingQueryRepositoryTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2031, 3, 3, 9, 0);

    private static DatabaseManager database;
    private static BookingQueryRepository queryRepository;
    private static BookingRepository bookingRepository;
    private static EmployeeRepository employeeRepository;
    private static WorkspaceRepository workspaceRepository;

    private static Employee petrov;
    private static Employee sidorov;
    private static Workspace desk;
    private static Workspace room;

    @BeforeAll
    static void setUp() {
        database = new DatabaseManager(System.getenv("TEST_DB_URL"),
                System.getenv().getOrDefault("TEST_DB_USER", "office"),
                System.getenv().getOrDefault("TEST_DB_PASSWORD", "office"));
        DatabaseMigrator.migrate(database);
        queryRepository = new BookingQueryRepository(database);
        bookingRepository = new BookingRepository(database);
        employeeRepository = new EmployeeRepository(database);
        workspaceRepository = new WorkspaceRepository(database);

        petrov = employeeRepository.save(new Employee(null, "Петров Пётр Петрович", "it.petrov@example.org",
                "Тестовый отдел", null, LocalDate.of(2020, 1, 1), false, EmployeeRole.EMPLOYEE));
        sidorov = employeeRepository.save(new Employee(null, "Сидоров Семён Семёнович", "it.sidorov@example.org",
                "Тестовый отдел", null, LocalDate.of(2021, 1, 1), false, EmployeeRole.EMPLOYEE));
        desk = workspaceRepository.save(new OpenDesk(null, "ZZ-91", 7, WorkspaceStatus.AVAILABLE, true));
        room = workspaceRepository.save(new MeetingRoom(null, "ZZ-92", 8, WorkspaceStatus.AVAILABLE, false, 10));

        bookingRepository.save(new Booking(petrov.getId(), desk.getId(), BASE, BASE.plusHours(2), null, "Планёрка 50%_тест"));
        bookingRepository.save(new Booking(sidorov.getId(), room.getId(), BASE.plusDays(1), BASE.plusDays(1).plusHours(4), 5, "Ретро"));
        Booking cancelled = bookingRepository.save(
                new Booking(petrov.getId(), room.getId(), BASE.plusDays(2), BASE.plusDays(2).plusHours(1), 3, "Отменённая"));
        bookingRepository.update(cancelled.withStatus(BookingStatus.CANCELLED));
    }

    @AfterAll
    static void tearDown() {
        for (Booking booking : bookingRepository.findAll()) {
            if (booking.getEmployeeId().equals(petrov.getId()) || booking.getEmployeeId().equals(sidorov.getId())) {
                bookingRepository.deleteById(booking.getId());
            }
        }
        employeeRepository.deleteById(petrov.getId());
        employeeRepository.deleteById(sidorov.getId());
        workspaceRepository.deleteById(desk.getId());
        workspaceRepository.deleteById(room.getId());
    }

    private List<BookingRow> mine(BookingFilter filter, BookingSort sort, boolean asc) {
        return queryRepository.search(filter, sort, asc).stream()
                .filter(row -> row.employeeId() == petrov.getId() || row.employeeId() == sidorov.getId())
                .toList();
    }

    private static BookingFilter text(String text) {
        return new BookingFilter(text, null, null, null, null, null);
    }

    @Test
    void emptyFilterReturnsAllBookings() {
        assertEquals(bookingRepository.findAll().size(),
                queryRepository.search(BookingFilter.empty(), BookingSort.START_TIME, true).size());
    }

    @Test
    void rowContainsJoinedEmployeeAndWorkspaceData() {
        BookingRow row = mine(text("ZZ-91"), BookingSort.START_TIME, true).get(0);

        assertEquals("Петров Пётр Петрович", row.employeeName());
        assertEquals("ZZ-91", row.workspaceCode());
        assertEquals(WorkspaceType.OPEN_DESK, row.workspaceType());
        assertEquals(7, row.floor());
    }

    @Test
    void textSearchMatchesName() {
        assertEquals(2, mine(text("петров"), BookingSort.START_TIME, true).size());
    }

    @Test
    void textSearchMatchesEmail() {
        assertEquals(1, mine(text("it.sidorov@"), BookingSort.START_TIME, true).size());
    }

    @Test
    void textSearchMatchesWorkspaceCode() {
        assertEquals(2, mine(text("zz-92"), BookingSort.START_TIME, true).size());
    }

    @Test
    void textSearchMatchesPurpose() {
        assertEquals(1, mine(text("ретро"), BookingSort.START_TIME, true).size());
    }

    @Test
    void percentAndUnderscoreInSearchAreTreatedLiterally() {
        assertEquals(1, mine(text("50%_тест"), BookingSort.START_TIME, true).size());
        assertEquals(0, mine(text("5_%тест"), BookingSort.START_TIME, true).size());
    }

    @Test
    void statusFilterWorks() {
        BookingFilter filter = new BookingFilter(null, BookingStatus.CANCELLED, null, null, null, null);

        List<BookingRow> rows = mine(filter, BookingSort.START_TIME, true);

        assertEquals(1, rows.size());
        assertEquals("Отменённая", rows.get(0).purpose());
    }

    @Test
    void workspaceTypeFilterWorks() {
        BookingFilter filter = new BookingFilter(null, null, WorkspaceType.MEETING_ROOM, null, null, null);

        assertEquals(2, mine(filter, BookingSort.START_TIME, true).size());
    }

    @Test
    void floorFilterWorks() {
        BookingFilter filter = new BookingFilter(null, null, null, 7, null, null);

        assertEquals(1, mine(filter, BookingSort.START_TIME, true).size());
    }

    @Test
    void periodFilterIncludesBoundaryDays() {
        BookingFilter filter = new BookingFilter(null, null, null, null, BASE.toLocalDate().plusDays(1),
                BASE.toLocalDate().plusDays(1));

        List<BookingRow> rows = mine(filter, BookingSort.START_TIME, true);

        assertEquals(1, rows.size());
        assertEquals("Ретро", rows.get(0).purpose());
    }

    @Test
    void filtersAreCombinedWithAnd() {
        BookingFilter filter = new BookingFilter("петров", null, WorkspaceType.MEETING_ROOM, null, null, null);

        assertEquals(1, mine(filter, BookingSort.START_TIME, true).size());
    }

    @Test
    void sortByStartTimeAscendingAndDescending() {
        List<BookingRow> asc = mine(text("zz-"), BookingSort.START_TIME, true);
        List<BookingRow> desc = mine(text("zz-"), BookingSort.START_TIME, false);

        assertEquals(asc.get(0).id(), desc.get(desc.size() - 1).id());
        assertTrue(asc.get(0).startTime().isBefore(asc.get(asc.size() - 1).startTime()));
    }

    @Test
    void sortByEmployeeName() {
        List<BookingRow> rows = mine(BookingFilter.empty(), BookingSort.EMPLOYEE_NAME, true);

        assertEquals("Петров Пётр Петрович", rows.get(0).employeeName());
        assertEquals("Сидоров Семён Семёнович", rows.get(rows.size() - 1).employeeName());
    }

    @Test
    void sortByDurationPutsLongestLast() {
        List<BookingRow> rows = mine(BookingFilter.empty(), BookingSort.DURATION, true);

        assertEquals(4, rows.get(rows.size() - 1).duration().toHours());
    }

    @Test
    void everySortOptionIsExecutable() {
        for (BookingSort sort : BookingSort.values()) {
            assertFalse(queryRepository.search(BookingFilter.empty(), sort, true).isEmpty());
            assertFalse(queryRepository.search(BookingFilter.empty(), sort, false).isEmpty());
        }
    }

    @Test
    void findRowByIdReturnsJoinedRow() {
        long id = mine(text("ретро"), BookingSort.START_TIME, true).get(0).id();

        assertEquals("ZZ-92", queryRepository.findRowById(id).orElseThrow().workspaceCode());
        assertTrue(queryRepository.findRowById(-1).isEmpty());
    }

    @Test
    void reportIsConsistent() {
        StatisticsService statistics = new StatisticsService(employeeRepository, workspaceRepository,
                bookingRepository, queryRepository);

        StatisticsReport report = statistics.buildReport(BASE.toLocalDate(), BASE.toLocalDate().plusDays(2));

        assertEquals(bookingRepository.findAll().size(), report.totalBookings());
        assertEquals(report.totalBookings(), report.byStatus().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(report.totalBookings(), report.byWorkspaceType().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(3, report.bookingsInPeriod());
        assertTrue(report.minDurationHours() <= report.averageDurationHours());
        assertTrue(report.averageDurationHours() <= report.maxDurationHours());
        assertTrue(report.noShowPercent() >= 0 && report.noShowPercent() <= 100);
        Map<BookingStatus, Long> byStatus = report.byStatus();
        assertTrue(byStatus.get(BookingStatus.CANCELLED) >= 1);
    }

    @Test
    void databaseRejectsOverlappingBookingOfSameWorkspace() {
        Booking overlapping = new Booking(sidorov.getId(), desk.getId(), BASE.plusHours(1), BASE.plusHours(3), null, null);

        assertThrows(BookingConflictException.class, () -> bookingRepository.save(overlapping));
    }

    @Test
    void databaseRejectsOverlappingBookingOfSameEmployee() {
        Booking overlapping = new Booking(petrov.getId(), room.getId(), BASE.plusHours(1), BASE.plusHours(2), 2, null);

        assertThrows(BookingConflictException.class, () -> bookingRepository.save(overlapping));
    }

    @Test
    void databaseAllowsAdjacentBookings() {
        Booking adjacent = bookingRepository.save(
                new Booking(sidorov.getId(), desk.getId(), BASE.plusHours(2), BASE.plusHours(3), null, null));

        bookingRepository.deleteById(adjacent.getId());
    }

    @Test
    void databaseIgnoresNotActiveBookingsWhenCheckingOverlap() {
        Booking cancelled = bookingRepository.save(new Booking(null, sidorov.getId(), desk.getId(), BASE, BASE.plusHours(1),
                BookingStatus.CANCELLED, null, null, null));

        bookingRepository.deleteById(cancelled.getId());
    }
}
