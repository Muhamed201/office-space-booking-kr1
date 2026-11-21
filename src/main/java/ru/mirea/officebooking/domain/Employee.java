package ru.mirea.officebooking.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "employee")
public class Employee {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;

  @Column(name = "email", nullable = false, unique = true, length = 150)
  private String email;

  @Column(name = "department", nullable = false, length = 100)
  private String department;

  @Column(name = "position", length = 100)
  private String position;

  @Column(name = "hire_date", nullable = false)
  private LocalDate hireDate;

  @Column(name = "blocked", nullable = false)
  private Boolean blocked = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public Employee() {}

  public Employee(String fullName, String email, String department, String position, LocalDate hireDate) {
    this.id = UUID.randomUUID();
    this.fullName = fullName;
    this.email = email.toLowerCase();
    this.department = department;
    this.position = position;
    this.hireDate = hireDate;
    this.blocked = false;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // Геттеры и сеттеры
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email.toLowerCase(); }
  public String getDepartment() { return department; }
  public void setDepartment(String department) { this.department = department; }
  public String getPosition() { return position; }
  public void setPosition(String position) { this.position = position; }
  public LocalDate getHireDate() { return hireDate; }
  public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
  public Boolean getBlocked() { return blocked; }
  public void setBlocked(Boolean blocked) { this.blocked = blocked; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  public void block() {
    this.blocked = true;
    this.updatedAt = LocalDateTime.now();
  }

  public void unblock() {
    this.blocked = false;
    this.updatedAt = LocalDateTime.now();
  }

  @PrePersist
  public void prePersist() {
    if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Employee employee = (Employee) o;
    return Objects.equals(id, employee.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}