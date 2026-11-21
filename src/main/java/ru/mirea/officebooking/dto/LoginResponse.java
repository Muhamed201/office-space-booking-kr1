package ru.mirea.officebooking.dto;

public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private boolean temporaryPassword;

    public LoginResponse() {}

    public LoginResponse(String accessToken, String refreshToken, boolean temporaryPassword) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.temporaryPassword = temporaryPassword;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public boolean isTemporaryPassword() { return temporaryPassword; }
    public void setTemporaryPassword(boolean temporaryPassword) { this.temporaryPassword = temporaryPassword; }
}
