package by.bsuir.filmcatalog.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Утилита для работы с JWT.
 *
 * Access token:
 *   - Подписанный HS256 JWT
 *   - Живёт 15 минут
 *   - Содержит username и userId в claims
 *
 * Refresh token:
 *   - Случайный UUID (хранится в БД, не является JWT)
 *   - Живёт 7 дней
 *   - При logout — отзывается (revoked=true)
 */
@Component
public class JwtUtils {

    /** Секретный ключ — минимум 256 бит, задаётся в application.properties */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /** Время жизни access token в миллисекундах (15 мин = 900_000) */
    @Value("${app.jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ========================
    // Access Token
    // ========================

    /**
     * Генерирует access token для пользователя.
     */
    public String generateAccessToken(String username, Long userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Извлекает username из access token.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Извлекает userId из access token.
     */
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    /**
     * Проверяет подпись и срок действия access token.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ========================
    // Refresh Token
    // ========================

    /**
     * Генерирует случайный UUID для refresh token.
     * Сам токен хранится в таблице refresh_tokens.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}
