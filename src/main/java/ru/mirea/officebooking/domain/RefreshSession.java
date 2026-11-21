package ru.mirea.officebooking.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refresh_session")
public class RefreshSession {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "refresh_token", nullable = false, unique = true, length = 500)
  private String refreshToken;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "revoked", nullable = false)
  private Boolean revoked = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public RefreshSession() {}

  public RefreshSession(UserAccount user, String refreshToken, LocalDateTime expiresAt) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.refreshToken = refreshToken;
    this.expiresAt = expiresAt;
    this.revoked = false;
    this.createdAt = LocalDateTime.now();
  }

  // Геттеры и сеттеры
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UserAccount getUser() { return user; }
  public void setUser(UserAccount user) { this.user = user; }
  public String getRefreshToken() { return refreshToken; }
  public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
  public Boolean getRevoked() { return revoked; }
  public void setRevoked(Boolean revoked) { this.revoked = revoked; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public void revoke() {
    this.revoked = true;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  public boolean isValid() {
    return !revoked && !isExpired();
  }

  @PrePersist
  public void prePersist() {
    if (this.createdAt == null) this.createdAt = LocalDateTime.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RefreshSession that = (RefreshSession) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}