package ru.mirea.officebooking.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.mirea.officebooking.dto.ErrorResponse;
import ru.mirea.officebooking.error.AuthenticationException;
import ru.mirea.officebooking.repository.UserAccountRepository;
import ru.mirea.officebooking.service.AuthService;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain swaggerSecurity(HttpSecurity http, AuthService auth, JwtProvider jwt, UserAccountRepository users)
            throws Exception {
        return http.securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, users), UsernamePasswordAuthenticationFilter.class)
                .authenticationManager(authentication -> {
                    try {
                        CurrentUser admin = auth.authenticateSwagger(authentication.getName(),
                                authentication.getCredentials().toString());
                        return UsernamePasswordAuthenticationToken.authenticated(admin, null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                    } catch (AuthenticationException e) {
                        throw new BadCredentialsException("Для Swagger требуется вход администратора", e);
                    }
                })
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(authz -> authz.anyRequest().access((authentication, context) ->
                        new AuthorizationDecision(authentication.get().getPrincipal() instanceof CurrentUser user
                                && !user.temporaryPassword() && user.role() == ru.mirea.officebooking.domain.Role.ADMIN)))
                .build();
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtProvider jwt, UserAccountRepository users, ObjectMapper mapper)
            throws Exception {
        return http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(mapper, request, response, HttpStatus.UNAUTHORIZED, "Войдите в систему"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(mapper, request, response, HttpStatus.FORBIDDEN, "Недостаточно прав")))
                .authorizeHttpRequests(authz -> authz
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
                        .anyRequest().access((authentication, context) -> {
                            if (!(authentication.get().getPrincipal() instanceof CurrentUser user) || user.temporaryPassword()) {
                                return new AuthorizationDecision(false);
                            }
                            var request = context.getRequest();
                            String path = request.getRequestURI().substring(request.getContextPath().length());
                            boolean adminOnly = path.startsWith("/api/admin/")
                                    || (path.startsWith("/api/workspaces") && !request.getMethod().equals("GET"));
                            return new AuthorizationDecision(!adminOnly ||
                                    user.role() == ru.mirea.officebooking.domain.Role.ADMIN);
                        }))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, users), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorResponse(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
