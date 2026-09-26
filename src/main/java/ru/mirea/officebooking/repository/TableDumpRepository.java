package ru.mirea.officebooking.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import ru.mirea.officebooking.model.TableDump;
import ru.mirea.officebooking.util.DatabaseManager;

public class TableDumpRepository {

    private static final List<String> TABLES = List.of("employee", "workspace", "booking", "flyway_schema_history");

    private final DatabaseManager databaseManager;

    public TableDumpRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<String> tableNames() {
        return TABLES;
    }

    public TableDump dump(String tableName) {
        if (!TABLES.contains(tableName)) {
            throw new IllegalArgumentException("Таблица не входит в список разрешённых: " + tableName);
        }
        String sql = "SELECT * FROM " + tableName + " ORDER BY 1";
        try (Connection conn = databaseManager.getConnection();
             Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(meta.getColumnName(i));
            }
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(String.valueOf(rs.getObject(i)));
                }
                rows.add(row);
            }
            return new TableDump(tableName, columns, rows);
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось прочитать таблицу " + tableName, e);
        }
    }
}
