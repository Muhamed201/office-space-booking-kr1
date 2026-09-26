package ru.mirea.officebooking.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.mirea.officebooking.exception.DatabaseException;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.util.DatabaseManager;

public class BookingRepository implements CrudRepository<Booking, Long> {

    private final DatabaseManager databaseManager;

    public BookingRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Booking save(Booking booking) {
        String sql = """
                INSERT INTO booking (employee_id, workspace_id, start_time, end_time, status, attendees, purpose)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        long newId;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(ps, booking);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("База данных не вернула id созданной брони");
                }
                newId = keys.getLong(1);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось сохранить бронь", e);
        }
        return findById(newId).orElseGet(() -> booking.withId(newId));
    }

    @Override
    public Optional<Booking> findById(Long id) {
        List<Booking> found = queryList("SELECT * FROM booking WHERE id = ?", ps -> ps.setLong(1, id),
                "Не удалось найти бронь id=" + id);
        return found.stream().findFirst();
    }

    @Override
    public List<Booking> findAll() {
        return queryList("SELECT * FROM booking ORDER BY id", ps -> {
        }, "Не удалось получить список броней");
    }

    @Override
    public void update(Booking booking) {
        String sql = """
                UPDATE booking
                SET employee_id = ?, workspace_id = ?, start_time = ?, end_time = ?,
                    status = ?, attendees = ?, purpose = ?
                WHERE id = ?
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFields(ps, booking);
            ps.setLong(8, booking.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось обновить бронь id=" + booking.getId(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM booking WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось удалить бронь id=" + id, e);
        }
    }

    public List<Booking> findActiveOverlappingForWorkspace(long workspaceId, LocalDateTime start, LocalDateTime end,
                                                           Long excludeBookingId) {
        String sql = """
                SELECT * FROM booking
                WHERE workspace_id = ? AND status = 'ACTIVE' AND start_time < ? AND ? < end_time
                  AND (? IS NULL OR id <> ?)
                ORDER BY start_time
                """;
        return queryList(sql, ps -> {
            ps.setLong(1, workspaceId);
            ps.setObject(2, end);
            ps.setObject(3, start);
            ps.setObject(4, excludeBookingId, Types.BIGINT);
            ps.setObject(5, excludeBookingId, Types.BIGINT);
        }, "Не удалось проверить пересечения броней рабочего места");
    }

    public List<Booking> findActiveOverlappingForEmployee(long employeeId, LocalDateTime start, LocalDateTime end,
                                                          Long excludeBookingId) {
        String sql = """
                SELECT * FROM booking
                WHERE employee_id = ? AND status = 'ACTIVE' AND start_time < ? AND ? < end_time
                  AND (? IS NULL OR id <> ?)
                ORDER BY start_time
                """;
        return queryList(sql, ps -> {
            ps.setLong(1, employeeId);
            ps.setObject(2, end);
            ps.setObject(3, start);
            ps.setObject(4, excludeBookingId, Types.BIGINT);
            ps.setObject(5, excludeBookingId, Types.BIGINT);
        }, "Не удалось проверить пересечения броней сотрудника");
    }

    public long countByEmployeeId(long employeeId) {
        return count("SELECT count(*) FROM booking WHERE employee_id = ?", ps -> ps.setLong(1, employeeId));
    }

    public long countByWorkspaceId(long workspaceId) {
        return count("SELECT count(*) FROM booking WHERE workspace_id = ?", ps -> ps.setLong(1, workspaceId));
    }

    public long countFutureActiveByWorkspaceId(long workspaceId, LocalDateTime now) {
        String sql = "SELECT count(*) FROM booking WHERE workspace_id = ? AND status = 'ACTIVE' AND end_time > ?";
        return count(sql, ps -> {
            ps.setLong(1, workspaceId);
            ps.setObject(2, now);
        });
    }

    public List<Booking> searchByEmployeeNameOrEmail(String substring) {
        String sql = """
                SELECT b.* FROM booking b
                JOIN employee e ON e.id = b.employee_id
                WHERE lower(e.full_name) LIKE ? ESCAPE '\\' OR lower(e.email) LIKE ? ESCAPE '\\'
                ORDER BY b.id
                """;
        String pattern = likePattern(substring);
        return queryList(sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
        }, "Не удалось выполнить поиск броней по сотруднику");
    }

    public List<Booking> searchByWorkspaceCode(String substring) {
        String sql = """
                SELECT b.* FROM booking b
                JOIN workspace w ON w.id = b.workspace_id
                WHERE lower(w.code) LIKE ? ESCAPE '\\'
                ORDER BY b.id
                """;
        String pattern = likePattern(substring);
        return queryList(sql, ps -> ps.setString(1, pattern), "Не удалось выполнить поиск броней по месту");
    }

    public List<Booking> searchByDate(LocalDate day) {
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        String sql = "SELECT * FROM booking WHERE start_time < ? AND ? < end_time ORDER BY id";
        return queryList(sql, ps -> {
            ps.setObject(1, dayEnd);
            ps.setObject(2, dayStart);
        }, "Не удалось выполнить поиск броней по дате");
    }

    public List<Booking> filterByStatus(BookingStatus status) {
        return queryList("SELECT * FROM booking WHERE status = ? ORDER BY id", ps -> ps.setString(1, status.name()),
                "Не удалось отфильтровать брони по статусу");
    }

    public List<Booking> filterByWorkspaceType(WorkspaceType type) {
        String sql = """
                SELECT b.* FROM booking b
                JOIN workspace w ON w.id = b.workspace_id
                WHERE w.type = ?
                ORDER BY b.id
                """;
        return queryList(sql, ps -> ps.setString(1, type.name()), "Не удалось отфильтровать брони по типу места");
    }

    public List<Booking> filterByFloor(int floor) {
        String sql = """
                SELECT b.* FROM booking b
                JOIN workspace w ON w.id = b.workspace_id
                WHERE w.floor = ?
                ORDER BY b.id
                """;
        return queryList(sql, ps -> ps.setInt(1, floor), "Не удалось отфильтровать брони по этажу");
    }

    public List<Booking> filterByDateRange(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT * FROM booking WHERE start_time < ? AND ? < end_time ORDER BY id";
        return queryList(sql, ps -> {
            ps.setObject(1, to);
            ps.setObject(2, from);
        }, "Не удалось отфильтровать брони по диапазону дат");
    }

    private static String likePattern(String substring) {
        String escaped = substring.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private long count(String sql, SqlBinder binder) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось посчитать брони", e);
        }
    }

    private List<Booking> queryList(String sql, SqlBinder binder, String errorMessage) {
        List<Booking> result = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(errorMessage, e);
        }
    }

    private void bindFields(PreparedStatement ps, Booking booking) throws SQLException {
        ps.setLong(1, booking.getEmployeeId());
        ps.setLong(2, booking.getWorkspaceId());
        ps.setObject(3, booking.getStartTime());
        ps.setObject(4, booking.getEndTime());
        ps.setString(5, booking.getStatus().name());
        ps.setObject(6, booking.getAttendees(), Types.INTEGER);
        ps.setString(7, booking.getPurpose());
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
        return new Booking(
                rs.getLong("id"),
                rs.getLong("employee_id"),
                rs.getLong("workspace_id"),
                rs.getObject("start_time", LocalDateTime.class),
                rs.getObject("end_time", LocalDateTime.class),
                BookingStatus.valueOf(rs.getString("status")),
                (Integer) rs.getObject("attendees"),
                rs.getString("purpose"),
                createdAt == null ? null : createdAt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        );
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
