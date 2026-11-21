package ru.mirea.officebooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "Текущий пароль обязателен")
    @Size(max = 72)
    private String currentPassword;

    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 8, max = 72, message = "Допустимо от 8 до 72 символов")
    private String newPassword;

    public ChangePasswordRequest() {}

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
