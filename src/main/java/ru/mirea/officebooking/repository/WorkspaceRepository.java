package ru.mirea.officebooking.repository;

import ru.mirea.officebooking.model.*;
import ru.mirea.officebooking.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkspaceRepository {
    private final DatabaseManager dbManager;

    public WorkspaceRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // Маппинг дискриминатора type -> нужный подкласс
    private Workspace mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String name = rs.getString("name");
        WorkspaceStatus status = WorkspaceStatus.valueOf(rs.getString("status"));
        String typeStr = rs.getString("type");
        int capacity = rs.getInt("capacity");

        WorkspaceType type = WorkspaceType.valueOf(typeStr);
        return switch (type) {
            case OPEN_DESK -> new OpenDesk(id, name, status);
            case MEETING_ROOM -> new MeetingRoom(id, name, status, capacity);
            case PHONE_BOOTH -> new PhoneBooth(id, name, status, capacity);
        };
    }

    public List<Workspace> findAll() {
        List<Workspace> workspaces = new ArrayList<>();
        String sql = "SELECT * FROM workspaces";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                workspaces.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка рабочих мест", e);
        }
        return workspaces;
    }

    public Optional<Workspace> findById(Long id) {
        String sql = "SELECT * FROM workspaces WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске рабочего места", e);
        }
        return Optional.empty();
    }

    public void save(Workspace workspace) {
        String sql = "INSERT INTO workspaces (name, type, status, capacity) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, workspace.getName());
            pstmt.setString(2, workspace.getType().name());
            pstmt.setString(3, workspace.getStatus().name());
            
            if (workspace instanceof MeetingRoom mr) {
                pstmt.setInt(4, mr.getCapacity());
            } else if (workspace instanceof PhoneBooth pb) {
                pstmt.setInt(4, pb.getCapacity());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    workspace.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении рабочего места", e);
        }
    }

    public void update(Workspace workspace) {
        String sql = "UPDATE workspaces SET name = ?, status = ?, capacity = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workspace.getName());
            pstmt.setString(2, workspace.getStatus().name());
            if (workspace instanceof MeetingRoom mr) {
                pstmt.setInt(3, mr.getCapacity());
            } else if (workspace instanceof PhoneBooth pb) {
                pstmt.setInt(3, pb.getCapacity());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setLong(4, workspace.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обновлении рабочего места", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM workspaces WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении рабочего места", e);
        }
    }
}