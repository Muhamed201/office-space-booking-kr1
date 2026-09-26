package ru.mirea.officebooking.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.mirea.officebooking.exception.DatabaseException;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.util.DatabaseManager;

public class EmployeeRepository implements CrudRepository<Employee, Long> {

    private final DatabaseManager databaseManager;

    public EmployeeRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Employee save(Employee employee) {
        String sql = """
                INSERT INTO employee (full_name, email, department, position, hire_date, blocked, role)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(ps, employee);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("База данных не вернула id созданного сотрудника");
                }
                return employee.withId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось сохранить сотрудника", e);
        }
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return findOne("SELECT * FROM employee WHERE id = ?", ps -> ps.setLong(1, id),
                "Не удалось найти сотрудника id=" + id);
    }

    public Optional<Employee> findByEmail(String email) {
        return findOne("SELECT * FROM employee WHERE email = ?", ps -> ps.setString(1, email),
                "Не удалось найти сотрудника по email");
    }

    @Override
    public List<Employee> findAll() {
        String sql = "SELECT * FROM employee ORDER BY id";
        List<Employee> result = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось получить список сотрудников", e);
        }
    }

    @Override
    public void update(Employee employee) {
        String sql = """
                UPDATE employee
                SET full_name = ?, email = ?, department = ?, position = ?, hire_date = ?, blocked = ?, role = ?
                WHERE id = ?
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFields(ps, employee);
            ps.setLong(8, employee.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось обновить сотрудника id=" + employee.getId(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM employee WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("Не удалось удалить сотрудника id=" + id, e);
        }
    }

    private Optional<Employee> findOne(String sql, SqlBinder binder, String errorMessage) {
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

    private void bindFields(PreparedStatement ps, Employee employee) throws SQLException {
        ps.setString(1, employee.getFullName());
        ps.setString(2, employee.getEmail());
        ps.setString(3, employee.getDepartment());
        ps.setString(4, employee.getPosition());
        ps.setObject(5, employee.getHireDate());
        ps.setBoolean(6, employee.isBlocked());
        ps.setString(7, employee.getRole().name());
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("department"),
                rs.getString("position"),
                rs.getObject("hire_date", LocalDate.class),
                rs.getBoolean("blocked"),
                EmployeeRole.valueOf(rs.getString("role"))
        );
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
