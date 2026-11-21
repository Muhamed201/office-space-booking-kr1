package ru.mirea.officebooking.dto;

import ru.mirea.officebooking.domain.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDto {
    private UUID id;
    private String email;
    private String name;
    private Role role;
    private Boolean active;
    private Boolean temporaryPassword;
    private LocalDateTime createdAt;

    public UserDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getTemporaryPassword() { return temporaryPassword; }
    public void setTemporaryPassword(Boolean temporaryPassword) { this.temporaryPassword = temporaryPassword; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
