package ru.mirea.officebooking.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_account")
public class UserAccount {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false, unique = true)
  private Employee employee;

  @Column(name = "email", nullable = false, unique = true, length = 150)
  private String email;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Column(name = "temporary_password", nullable = false)
  private Boolean temporaryPassword = true;

  @Column(name = "failed_login_attempts", nullable = false)
  private Integer failedLoginAttempts = 0;

  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "auth_version", nullable = false)
  private Long authVersion = 0L;

  public Long getAuthVersion() { return authVersion; }
  public void setAuthVersion(Long authVersion) { this.authVersion = authVersion; }

  public UserAccount() {}

  public UserAccount(Employee employee, String passwordHash, Role role) {
    this.id = UUID.randomUUID();
    this.employee = employee;
    this.email = employee.getEmail();
    this.name = employee.getFullName();
    this.passwordHash = passwordHash;
    this.role = role;
    this.active = true;
    this.temporaryPassword = true;
    this.failedLoginAttempts = 0;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // Геттеры и сеттеры
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Employee getEmployee() { return employee; }
  public void setEmployee(Employee employee) { this.employee = employee; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }
  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }
  public Boolean getTemporaryPassword() { return temporaryPassword; }
  public void setTemporaryPassword(Boolean temporaryPassword) { this.temporaryPassword = temporaryPassword; }
  public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
  public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
  public LocalDateTime getLockedUntil() { return lockedUntil; }
  public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  public void incrementFailedAttempts() {
    this.failedLoginAttempts++;
    this.updatedAt = LocalDateTime.now();
  }

  public void resetFailedAttempts() {
    this.failedLoginAttempts = 0;
    this.lockedUntil = null;
    this.updatedAt = LocalDateTime.now();
  }

  public void lockForMinutes(int minutes) {
    this.lockedUntil = LocalDateTime.now().plusMinutes(minutes);
    this.updatedAt = LocalDateTime.now();
  }

  public boolean isLocked() {
    return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
  }

  public void markPasswordAsPermanent() {
    this.temporaryPassword = false;
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
    UserAccount that = (UserAccount) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}