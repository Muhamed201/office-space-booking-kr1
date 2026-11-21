package ru.mirea.officebooking.security;

import org.springframework.security.core.context.SecurityContextHolder;
import ru.mirea.officebooking.domain.Role;

import java.util.UUID;

public record CurrentUser(UUID id, UUID employeeId, Role role, boolean temporaryPassword) {
    public static CurrentUser get() {
        return (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static CurrentUser optional() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof CurrentUser user ? user : null;
    }
}
