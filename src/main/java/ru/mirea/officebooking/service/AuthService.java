package ru.mirea.officebooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.officebooking.domain.*;
import ru.mirea.officebooking.dto.*;
import ru.mirea.officebooking.error.AuthenticationException;
import ru.mirea.officebooking.error.ValidationException;
import ru.mirea.officebooking.repository.*;
import ru.mirea.officebooking.security.CurrentUser;
import ru.mirea.officebooking.security.JwtProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtProvider jwt;
    private final AuditService audit;
    private final int maxAttempts;
    private final int lockoutMinutes;
    private final int refreshDays;
    private final boolean permanentDelete;

    public AuthService(UserAccountRepository users, RefreshSessionRepository sessions,
                       PasswordEncoder encoder, JwtProvider jwt, AuditService audit,
                       @Value("${app.security.max-login-attempts}") int maxAttempts,
                       @Value("${app.security.lockout-minutes}") int lockoutMinutes,
                       @Value("${app.jwt.refresh-days}") int refreshDays,
                       @Value("${app.security.permanent-delete-enabled}") boolean permanentDelete) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        if (maxAttempts <= 0 || lockoutMinutes <= 0 || refreshDays <= 0) {
            throw new IllegalArgumentException("Настройки блокировки и срока сессии должны быть положительными");
        }
        this.maxAttempts = maxAttempts;
        this.lockoutMinutes = lockoutMinutes;
        this.refreshDays = refreshDays;
        this.permanentDelete = permanentDelete;
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public LoginResponse login(LoginRequest request) {
        UserAccount user = verifyCredentials(request.getEmail(), request.getPassword());
        audit.record(user, EventType.LOGIN, "UserAccount", user.getId(), "Вход в систему");
        return issueSession(user);
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public CurrentUser authenticateSwagger(String email, String password) {
        UserAccount user = verifyCredentials(email, password);
        if (user.getRole() != Role.ADMIN || user.getTemporaryPassword()) {
            throw new AuthenticationException("Для Swagger нужна учётная запись администратора с постоянным паролем");
        }
        return new CurrentUser(user.getId(), user.getEmployee().getId(), user.getRole(), false);
    }

    private UserAccount verifyCredentials(String email, String password) {
        UserAccount user = users.findLockedByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || !user.getActive() || user.isLocked()) {
            audit.record(user, EventType.LOGIN_FAILED, "UserAccount", user == null ? null : user.getId(),
                    "Неудачная попытка входа");
            throw new AuthenticationException("Неверный email или пароль, либо вход временно недоступен");
        }
        if (!matches(password, user)) {
            if (user.getLockedUntil() != null) {
                user.resetFailedAttempts();
            }
            user.incrementFailedAttempts();
            if (user.getFailedLoginAttempts() >= maxAttempts) {
                user.lockForMinutes(lockoutMinutes);
            }
            audit.record(user, EventType.LOGIN_FAILED, "UserAccount", user.getId(), "Неудачная попытка входа");
            throw new AuthenticationException("Неверный email или пароль, либо вход временно недоступен");
        }
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.resetFailedAttempts();
        }
        return user;
    }

    public LoginResponse refresh(String token) {
        RefreshSession session = session(token);
        UserAccount user = session.getUser();
        if (!session.isValid() || !user.getActive()) {
            throw invalidSession();
        }
        session.revoke();
        return issueSession(user);
    }

    public void logout(String token) {
        RefreshSession session = session(token);
        if (!session.getRevoked()) {
            session.revoke();
            audit.record(session.getUser(), EventType.LOGOUT, "UserAccount", session.getUser().getId(), "Выход из системы");
        }
    }

    private RefreshSession session(String token) {
        if (token == null || token.length() != 43) {
            throw invalidSession();
        }
        String digest = hash(token);
        UUID owner = sessions.findOwnerId(digest).orElseThrow(this::invalidSession);
        users.findLockedById(owner).orElseThrow(this::invalidSession);
        return sessions.findByRefreshToken(digest).orElseThrow(this::invalidSession);
    }

    public LoginResponse changePassword(ChangePasswordRequest request) {
        UserAccount user = users.findLockedById(CurrentUser.get().id()).orElseThrow(this::invalidSession);
        if (!matches(request.getCurrentPassword(), user)) {
            throw new ValidationException("Текущий пароль неверен");
        }
        validatePassword(request.getNewPassword());
        if (matches(request.getNewPassword(), user)) {
            throw new ValidationException("Новый пароль должен отличаться от текущего");
        }
        setPassword(user, request.getNewPassword(), false);
        audit.logEvent(EventType.UPDATE, "UserAccount", user.getId(), "Пароль изменён");
        return issueSession(user);
    }

    void setPassword(UserAccount user, String password, boolean temporary) {
        validatePassword(password);
        user.setPasswordHash(encoder.encode(password));
        user.setTemporaryPassword(temporary);
        user.resetFailedAttempts();
        invalidateSessions(user);
    }

    void invalidateSessions(UserAccount user) {
        user.setAuthVersion(user.getAuthVersion() + 1);
        sessions.findByUserIdAndRevokedFalse(user.getId()).forEach(RefreshSession::revoke);
    }

    @Transactional(readOnly = true)
    public CurrentUserDto getCurrentUser(UUID id) {
        UserAccount user = users.findById(id).orElseThrow(this::invalidSession);
        var dto = new CurrentUserDto(id, user.getEmail(), user.getName(), user.getRole(), user.getTemporaryPassword());
        dto.setCanDeleteBookings(permanentDelete && user.getRole() == Role.ADMIN && !user.getTemporaryPassword());
        return dto;
    }

    public static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8 ||
                password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Пароль должен содержать минимум 8 символов и не больше 72 байт");
        }
    }

    static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean matches(String password, UserAccount user) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72 && encoder.matches(password, user.getPasswordHash());
    }

    private LoginResponse issueSession(UserAccount user) {
        String refresh = randomToken();
        sessions.save(new RefreshSession(user, hash(refresh), LocalDateTime.now().plusDays(refreshDays)));
        return new LoginResponse(jwt.generateAccessToken(user.getId(), user.getAuthVersion()), refresh,
                user.getTemporaryPassword());
    }

    private AuthenticationException invalidSession() {
        return new AuthenticationException("Сессия недействительна или истекла");
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
