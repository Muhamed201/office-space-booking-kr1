package ru.mirea.officebooking.model;

import java.time.LocalDate;
import java.util.Objects;

public class Employee {
    private Long id;
    private String fullName;
    private String email;
    private String department;
    private String position;
    private LocalDate hireDate;
    private LocalDate birthDate; // Добавлено для проверки "возраст >= 14"
    private boolean blocked;
    private EmployeeRole role;

    public Employee() {}

    public Employee(String fullName, String email, String department, String position,
                    LocalDate hireDate, LocalDate birthDate, boolean blocked, EmployeeRole role) {
        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.position = position;
        this.hireDate = hireDate;
        this.birthDate = birthDate;
        this.blocked = blocked;
        this.role = role;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public EmployeeRole getRole() { return role; }
    public void setRole(EmployeeRole role) { this.role = role; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return Objects.equals(id, employee.id) && Objects.equals(email, employee.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", position='" + position + '\'' +
                ", hireDate=" + hireDate +
                ", birthDate=" + birthDate +
                ", blocked=" + blocked +
                ", role=" + role +
                '}';
    }
}