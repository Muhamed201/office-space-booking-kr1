package ru.mirea.officebooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final String issuer;
    private final long accessMinutes;

    public JwtProvider(@Value("${app.jwt.secret-base64}") String secretBase64,
                       @Value("${app.jwt.issuer}") String issuer,
                       @Value("${app.jwt.access-minutes}") long accessMinutes) {
        if (issuer.isBlank() || accessMinutes <= 0) {
            throw new IllegalArgumentException("Для JWT укажите издателя и положительный срок действия");
        }
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
    }

    public String generateAccessToken(UUID userId, long authVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
                .claim("type", "access")
                .claim("version", authVersion)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith(secretKey).requireIssuer(issuer).require("type", "access")
                .build().parseSignedClaims(token).getPayload();
    }
}
