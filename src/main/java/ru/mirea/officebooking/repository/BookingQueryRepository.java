package ru.mirea.officebooking.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.mirea.officebooking.dto.BookingFilter;
import ru.mirea.officebooking.dto.BookingRow;
import ru.mirea.officebooking.dto.BookingSort;
import ru.mirea.officebooking.dto.StatisticsReport.NamedCount;
import ru.mirea.officebooking.model.BookingStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.util.DatabaseManager;

public class BookingQueryRepository {

    public record DurationStats(double minHours, double avgHours, double maxHours) {
    }

    private static final String SELECT_ROWS = """
            SELECT b.id, b.employee_id, e.full_name, e.email,
                   b.workspace_id, w.code, w.type, w.floor,
                   b.start_time, b.end_time, b.status, b.attendees, b.purpose
            FROM booking b
            JOIN employee e ON e.id = b.employee_id
            JOIN workspace w ON w.id = b.workspace_id
            """;

    private final DatabaseManager databaseManager;

    public BookingQueryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<BookingRow> search(BookingFilter filter, BookingSort sort, boolean ascending) {
        StringBuilder sql = new StringBuilder(SELECT_ROWS);
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (filter.text() != null) {
            String pattern = likePattern(filter.text());
            conditions.add("(lower(e.full_name) LIKE ? ESCAPE '\\' OR lower(e.email) LIKE ? ESCAPE '\\'"
                    + " OR lower(w.code) LIKE ? ESCAPE '\\' OR lower(coalesce(b.purpose, '')) LIKE ? ESCAPE '\\')");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (filter.status() != null) {
            conditions.add("b.status = ?");
            params.add(filter.status().name());
        }
        if (filter.workspaceType() != null) {
            conditions.add("w.type = ?");
            params.add(filter.workspaceType().name());
        }
        if (filter.floor() != null) {
            conditions.add("w.floor = ?");
            params.add(filter.floor());
        }
        if (filter.dateFrom() != null) {
            conditions.add("b.end_time > ?");
            params.add(filter.dateFrom().atStartOfDay());
        }
        if (filter.dateTo() != null) {
            conditions.add("b.start_time < ?");
            params.add(filter.dateTo().plusDays(1).atStartOfDay());
        }
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join("\n  AND ", conditions)).append('\n');
        }
        sql.append("ORDER BY ").append(orderColumn(sort)).append(ascending ? " ASC" : " DESC").append(", b.id");

        return queryRows(sql.toString(), params, "Не удалось загрузить список броней");
    }

    public Optional<BookingRow> findRowById(long id) {
        return queryRows(SELECT_ROWS + "WHERE b.id = ?", List.of(id), "Не удалось найти бронь id=" + id)
                .stream().findFirst();
    }

    public List<Integer> distinctFloors() {
        List<Integer> floors = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT floor FROM workspace ORDER BY floor");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                floors.add(rs.getInt(1));
            }
            return floors;
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось получить список этажей", e);
        }
    }

    public long countAll() {
        return scalarLong("SELECT count(*) FROM booking", List.of(), "Не удалось посчитать брони");
    }

    public long countInPeriod(LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT count(*) FROM booking WHERE TRUE");
        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append(" AND end_time > ?");
            params.add(from.atStartOfDay());
        }
        if (to != null) {
            sql.append(" AND start_time < ?");
            params.add(to.plusDays(1).atStartOfDay());
        }
        return scalarLong(sql.toString(), params, "Не удалось посчитать брони за период");
    }

    public Map<BookingStatus, Long> countByStatus() {
        Map<BookingStatus, Long> result = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            result.put(status, 0L);
        }
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status, count(*) FROM booking GROUP BY status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(BookingStatus.valueOf(rs.getString(1)), rs.getLong(2));
            }
            return result;
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось посчитать брони по статусам", e);
        }
    }

    public Map<WorkspaceType, Long> countByWorkspaceType() {
        Map<WorkspaceType, Long> result = new EnumMap<>(WorkspaceType.class);
        for (WorkspaceType type : WorkspaceType.values()) {
            result.put(type, 0L);
        }
        String sql = """
                SELECT w.type, count(*) FROM booking b
                JOIN workspace w ON w.id = b.workspace_id
                GROUP BY w.type
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(WorkspaceType.valueOf(rs.getString(1)), rs.getLong(2));
            }
            return result;
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось посчитать брони по типам мест", e);
        }
    }

    public DurationStats durationStats() {
        String sql = """
                SELECT coalesce(min(EXTRACT(EPOCH FROM (end_time - start_time)) / 3600.0), 0),
                       coalesce(avg(EXTRACT(EPOCH FROM (end_time - start_time)) / 3600.0), 0),
                       coalesce(max(EXTRACT(EPOCH FROM (end_time - start_time)) / 3600.0), 0)
                FROM booking
                WHERE status <> 'CANCELLED'
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new DurationStats(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3));
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось посчитать длительность броней", e);
        }
    }

    public List<NamedCount> topWorkspaces(int limit) {
        String sql = """
                SELECT w.code, count(*) AS cnt FROM booking b
                JOIN workspace w ON w.id = b.workspace_id
                GROUP BY w.id, w.code
                ORDER BY cnt DESC, w.code
                LIMIT ?
                """;
        return topList(sql, limit, "Не удалось получить топ рабочих мест");
    }

    public List<NamedCount> topEmployees(int limit) {
        String sql = """
                SELECT e.full_name, count(*) AS cnt FROM booking b
                JOIN employee e ON e.id = b.employee_id
                GROUP BY e.id, e.full_name
                ORDER BY cnt DESC, e.full_name
                LIMIT ?
                """;
        return topList(sql, limit, "Не удалось получить топ сотрудников");
    }

    private List<NamedCount> topList(String sql, int limit, String errorMessage) {
        List<NamedCount> result = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new NamedCount(rs.getString(1), rs.getLong(2)));
                }
            }
            return result;
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(errorMessage, e);
        }
    }

    private long scalarLong(String sql, List<Object> params, String errorMessage) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(errorMessage, e);
        }
    }

    private List<BookingRow> queryRows(String sql, List<Object> params, String errorMessage) {
        List<BookingRow> result = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
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

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static BookingRow mapRow(ResultSet rs) throws SQLException {
        return new BookingRow(
                rs.getLong("id"),
                rs.getLong("employee_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getLong("workspace_id"),
                rs.getString("code"),
                WorkspaceType.valueOf(rs.getString("type")),
                rs.getInt("floor"),
                rs.getObject("start_time", LocalDateTime.class),
                rs.getObject("end_time", LocalDateTime.class),
                BookingStatus.valueOf(rs.getString("status")),
                (Integer) rs.getObject("attendees"),
                rs.getString("purpose"));
    }

    private static String orderColumn(BookingSort sort) {
        return switch (sort) {
            case START_TIME -> "b.start_time";
            case EMPLOYEE_NAME -> "e.full_name";
            case WORKSPACE_CODE -> "w.code";
            case DURATION -> "(b.end_time - b.start_time)";
            case STATUS -> "b.status";
        };
    }

    private static String likePattern(String substring) {
        String escaped = substring.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
