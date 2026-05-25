package by.bsuir.filmcatalog.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

// Утилита для работы с JWT токенами.
// Access token живёт 15 минут, refresh token — случайный UUID, хранится в БД.
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Время жизни access token (15 минут по умолчанию)
    @Value("${app.jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Создаём JWT с именем пользователя и его id
    public String generateAccessToken(String username, Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    // Проверяем подпись и срок действия токена
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

    // Refresh token — просто UUID, хранится в таблице refresh_tokens
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}
