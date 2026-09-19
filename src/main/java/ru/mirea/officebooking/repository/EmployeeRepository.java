package ru.mirea.officebooking.repository;

import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.EmployeeRole;
import ru.mirea.officebooking.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeRepository implements CrudRepository<Employee, Long> {
    private final DatabaseManager dbManager;

    public EmployeeRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Employee save(Employee employee) {
        String sql = "INSERT INTO employee (full_name, email, department, position, hire_date, birth_date, blocked, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try {
            Connection conn = dbManager.getConnection();
            // Не закрываем conn, так как DatabaseManager может использовать одно общее соединение
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                mapToStatement(ps, employee);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        employee.setId(rs.getLong("id"));
                    }
                }
            }
            return employee;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении сотрудника", e);
        }
    }

    @Override
    public Optional<Employee> findById(Long id) {
        String sql = "SELECT * FROM employee WHERE id = ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске сотрудника", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Employee> findAll() {
        String sql = "SELECT * FROM employee";
        List<Employee> employees = new ArrayList<>();
        try {
            Connection conn = dbManager.getConnection();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    employees.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка сотрудников", e);
        }
        return employees;
    }

    @Override
    public Employee update(Employee employee) {
        String sql = "UPDATE employee SET full_name=?, email=?, department=?, position=?, hire_date=?, birth_date=?, blocked=?, role=? WHERE id=?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                mapToStatement(ps, employee);
                ps.setLong(9, employee.getId());
                ps.executeUpdate();
            }
            return employee;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обновлении сотрудника", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM employee WHERE id = ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении сотрудника", e);
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM employee WHERE email = ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при проверке email", e);
        }
    }

    public boolean existsByEmail(String email, Long excludeId) {
        String sql = "SELECT 1 FROM employee WHERE email = ? AND id != ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setLong(2, excludeId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при проверке email", e);
        }
    }

    public boolean hasBookings(Long employeeId) {
        String sql = "SELECT 1 FROM booking WHERE employee_id = ? LIMIT 1";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при проверке броней", e);
        }
    }

    private void mapToStatement(PreparedStatement ps, Employee e) throws SQLException {
        ps.setString(1, e.getFullName());
        ps.setString(2, e.getEmail());
        ps.setString(3, e.getDepartment());
        ps.setString(4, e.getPosition());
        ps.setDate(5, e.getHireDate() != null ? Date.valueOf(e.getHireDate()) : null);
        ps.setDate(6, e.getBirthDate() != null ? Date.valueOf(e.getBirthDate()) : null);
        ps.setBoolean(7, e.isBlocked());
        ps.setString(8, e.getRole() != null ? e.getRole().name() : null);
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getLong("id"));
        e.setFullName(rs.getString("full_name"));
        e.setEmail(rs.getString("email"));
        e.setDepartment(rs.getString("department"));
        e.setPosition(rs.getString("position"));
        
        Date hireDate = rs.getDate("hire_date");
        if (hireDate != null) e.setHireDate(hireDate.toLocalDate());
        
        try {
            Date birthDate = rs.getDate("birth_date");
            if (birthDate != null) e.setBirthDate(birthDate.toLocalDate());
        } catch (SQLException ignore) {
            // Колонка может отсутствовать, если миграция V3 еще не прогнана
        }
        
        e.setBlocked(rs.getBoolean("blocked"));
        String roleStr = rs.getString("role");
        if (roleStr != null) e.setRole(EmployeeRole.valueOf(roleStr));
        return e;
    }
}