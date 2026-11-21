package ru.mirea.officebooking.dto;

import ru.mirea.officebooking.domain.Role;

import java.util.UUID;

public class CurrentUserDto {
    private UUID id;
    private String email;
    private String name;
    private Role role;
    private boolean temporaryPassword;

    public CurrentUserDto() {}

    public CurrentUserDto(UUID id, String email, String name, Role role, boolean temporaryPassword) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.temporaryPassword = temporaryPassword;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isTemporaryPassword() { return temporaryPassword; }
    public void setTemporaryPassword(boolean temporaryPassword) { this.temporaryPassword = temporaryPassword; }

    private boolean canDeleteBookings;
    public boolean isCanDeleteBookings() { return canDeleteBookings; }
    public void setCanDeleteBookings(boolean value) { canDeleteBookings = value; }
}
