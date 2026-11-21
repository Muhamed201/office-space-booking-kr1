package ru.mirea.officebooking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.mirea.officebooking.domain.*;
import ru.mirea.officebooking.dto.LoginRequest;
import ru.mirea.officebooking.error.AuthenticationException;
import ru.mirea.officebooking.filter.BookingFilter;
import ru.mirea.officebooking.repository.*;
import ru.mirea.officebooking.service.AuthService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OfficeBookingApplicationTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired EmployeeRepository employees;
    @Autowired WorkspaceRepository workspaces;
    @Autowired BookingRepository bookings;
    @Autowired UserAccountRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void contextStartsAndHealthIsPublic() throws Exception {
        http.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        http.perform(get("/api/bookings")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path").value("/api/bookings"));
    }

    @Test
    void priorityUsesDurationAttendeesAndFlags() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        var booking = new Booking("test", null, null, start, start.plusHours(3), BookingStatus.ACTIVE);
        booking.setAttendees(6);
        booking.setRequiresEquipment(true);
        booking.setIsRecurring(true);
        booking.calculatePriority();
        assertThat(booking.getPriority()).isEqualTo(10);
    }

    @Test
    void specificationSearchesLiteralWildcardsAndFiltersByOwnerAndDuration() {
        var owner = employee();
        var other = employee();
        var workspace = workspaces.save(new Workspace("T-FILTER", 1, WorkspaceType.OPEN_DESK, WorkspaceStatus.AVAILABLE));
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        var match = new Booking("T-F1", owner, workspace, start, start.plusHours(2), BookingStatus.ACTIVE);
        match.setDescription("Загрузка 90%");
        var falseMatch = new Booking("T-F2", owner, workspace, start, start.plusHours(2), BookingStatus.ACTIVE);
        falseMatch.setDescription("Загрузка 900");
        var foreign = new Booking("T-F3", other, workspace, start, start.plusHours(2), BookingStatus.ACTIVE);
        foreign.setDescription("Загрузка 90%");
        bookings.saveAllAndFlush(java.util.List.of(match, falseMatch, foreign));
        var filter = new BookingFilter("90%", workspace.getId(), BookingStatus.ACTIVE, null, null, null,
                null, false, null, 90L, 150L);
        assertThat(bookings.findAll(filter.specification(owner.getId())))
                .extracting(Booking::getId).containsExactly(match.getId());
    }

    @Test
    void failedLoginsPersistAndLockAccount() {
        var person = employee();
        var user = users.save(new UserAccount(person, passwords.encode("fixture-employee-password"), Role.EMPLOYEE));
        LoginRequest wrong = credentials(user.getEmail(), "wrong-password");
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> auth.login(wrong)).isInstanceOf(AuthenticationException.class);
        }
        var persisted = users.findById(user.getId()).orElseThrow();
        assertThat(persisted.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(persisted.isLocked()).isTrue();
        assertThatThrownBy(() -> auth.login(credentials(user.getEmail(), "fixture-employee-password")))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void temporaryPasswordAndRefreshTokensCannotAccessBookings() throws Exception {
        var person = employee();
        var user = users.save(new UserAccount(person, passwords.encode("fixture-temporary-password"), Role.EMPLOYEE));
        var tokens = auth.login(credentials(user.getEmail(), "fixture-temporary-password"));
        http.perform(get("/api/bookings").header("Authorization", "Bearer " + tokens.getAccessToken()))
                .andExpect(status().isForbidden());
        http.perform(get("/api/bookings").header("Authorization", "Bearer " + tokens.getRefreshToken()))
                .andExpect(status().isUnauthorized());
        var changed = http.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tokens.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", "fixture-temporary-password",
                                "newPassword", "fixture-permanent-password"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.temporaryPassword").value(false)).andReturn();
        String access = json.readTree(changed.getResponse().getContentAsString()).get("accessToken").asText();
        http.perform(get("/api/bookings").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
        http.perform(get("/api/admin/users").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> auth.refresh(tokens.getRefreshToken())).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void malformedAndInvalidWorkspaceFieldsNeverReachStorage() throws Exception {
        var user = new UserAccount(employee(), passwords.encode("fixture-validation-password"), Role.ADMIN);
        user.setTemporaryPassword(false);
        users.save(user);
        var token = auth.login(credentials(user.getEmail(), "fixture-validation-password")).getAccessToken();
        var valid = """
                {"code":"T-123","floor":2,"type":"MEETING_ROOM","status":"AVAILABLE","hasMonitor":true,"capacity":4}
                """;
        var invalid = java.util.List.of(
                valid.replace("T-123", "код без формата"),
                valid.replace("\"floor\":2", "\"floor\":2.8"),
                valid.replace("\"capacity\":4", "\"capacity\":\"4\""),
                valid.replace("\"capacity\":4", "\"capacity\":0"),
                valid.replace("\"capacity\":4", "\"capacity\":4,\"surprise\":true"),
                valid.replace("\"capacity\":4", "\"capacity\":4,\"capacity\":5"),
                valid.replace("\"MEETING_ROOM\"", "1"),
                valid.replace("MEETING_ROOM", "OPEN_DESK"),
                valid.replace("\"hasMonitor\":true", "\"hasMonitor\":null"),
                valid.replace("\"hasMonitor\":true", "\"hasMonitor\":\"true\""));
        for (String body : invalid) {
            http.perform(post("/api/workspaces").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.path").value("/api/workspaces"));
        }
        assertThat(workspaces.findByCode("T-123")).isEmpty();
        http.perform(post("/api/workspaces").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isCreated());
        assertThat(workspaces.findByCode("T-123")).isPresent();
    }

    private Employee employee() {
        return employees.save(new Employee("Тестовый сотрудник", UUID.randomUUID() + "@test.example",
                "Тесты", null, LocalDate.of(2020, 1, 1)));
    }

    private LoginRequest credentials(String email, String password) {
        var login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);
        return login;
    }
}
