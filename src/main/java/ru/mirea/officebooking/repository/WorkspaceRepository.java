package ru.mirea.officebooking.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.mirea.officebooking.exception.DatabaseException;
import ru.mirea.officebooking.model.MeetingRoom;
import ru.mirea.officebooking.model.OpenDesk;
import ru.mirea.officebooking.model.PhoneBooth;
import ru.mirea.officebooking.model.Workspace;
import ru.mirea.officebooking.model.WorkspaceStatus;
import ru.mirea.officebooking.model.WorkspaceType;
import ru.mirea.officebooking.util.DatabaseManager;

public class WorkspaceRepository implements CrudRepository<Workspace, Long> {

    private final DatabaseManager databaseManager;

    public WorkspaceRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Workspace save(Workspace workspace) {
        String sql = """
                INSERT INTO workspace (code, floor, type, status, has_monitor, capacity)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(ps, workspace);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("База данных не вернула id созданного рабочего места");
                }
                return workspace.withId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось сохранить рабочее место", e);
        }
    }

    @Override
    public Optional<Workspace> findById(Long id) {
        return findOne("SELECT * FROM workspace WHERE id = ?", ps -> ps.setLong(1, id),
                "Не удалось найти рабочее место id=" + id);
    }

    public Optional<Workspace> findByCode(String code) {
        return findOne("SELECT * FROM workspace WHERE code = ?", ps -> ps.setString(1, code),
                "Не удалось найти рабочее место по коду");
    }

    @Override
    public List<Workspace> findAll() {
        String sql = "SELECT * FROM workspace ORDER BY id";
        List<Workspace> result = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось получить список рабочих мест", e);
        }
    }

    @Override
    public void update(Workspace workspace) {
        String sql = """
                UPDATE workspace
                SET code = ?, floor = ?, type = ?, status = ?, has_monitor = ?, capacity = ?
                WHERE id = ?
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFields(ps, workspace);
            ps.setLong(7, workspace.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось обновить рабочее место id=" + workspace.getId(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM workspace WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось удалить рабочее место id=" + id, e);
        }
    }

    private Optional<Workspace> findOne(String sql, SqlBinder binder, String errorMessage) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(errorMessage, e);
        }
    }

    private void bindFields(PreparedStatement ps, Workspace workspace) throws SQLException {
        ps.setString(1, workspace.getCode());
        ps.setInt(2, workspace.getFloor());
        ps.setString(3, workspace.type().name());
        ps.setString(4, workspace.getStatus().name());
        ps.setBoolean(5, workspace.isHasMonitor());
        ps.setInt(6, workspace.getCapacity());
    }

    private Workspace mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String code = rs.getString("code");
        int floor = rs.getInt("floor");
        WorkspaceStatus status = WorkspaceStatus.valueOf(rs.getString("status"));
        boolean hasMonitor = rs.getBoolean("has_monitor");
        WorkspaceType type = WorkspaceType.valueOf(rs.getString("type"));
        return switch (type) {
            case OPEN_DESK -> new OpenDesk(id, code, floor, status, hasMonitor);
            case PHONE_BOOTH -> new PhoneBooth(id, code, floor, status, hasMonitor);
            case MEETING_ROOM -> new MeetingRoom(id, code, floor, status, hasMonitor, rs.getInt("capacity"));
        };
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
