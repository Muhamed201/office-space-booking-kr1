package ru.mirea.officebooking.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.mirea.officebooking.repository.UserAccountRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final UserAccountRepository users;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserAccountRepository users) {
        this.jwtProvider = jwtProvider;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(header.substring(7));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            var claims = jwtProvider.parseAccessToken(token);
            var version = claims.get("version", Long.class);
            users.findById(UUID.fromString(claims.getSubject()))
                    .filter(user -> user.getActive() && user.getAuthVersion().equals(version))
                    .ifPresent(user -> {
                        var principal = new CurrentUser(user.getId(), user.getEmployee().getId(),
                                user.getRole(), user.getTemporaryPassword());
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                        SecurityContextHolder.getContext().setAuthentication(
                                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
                    });
        } catch (JwtException | IllegalArgumentException ignored) {
            // Spring Security отвечает за HTTP-ответ для неаутентифицированного запроса.
        }
    }
}
