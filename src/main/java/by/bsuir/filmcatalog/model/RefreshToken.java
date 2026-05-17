package by.bsuir.filmcatalog.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Refresh-токен, хранится в БД.
 * Позволяет безопасно отзывать сессии (logout) и обновлять access token.
 *
 * Структура таблицы:
 *   id          BIGINT PK
 *   token       TEXT UNIQUE NOT NULL    — UUID строка токена
 *   user_id     BIGINT FK -> users.id
 *   expires_at  DATETIME NOT NULL
 *   revoked     BOOLEAN NOT NULL
 *   created_at  DATETIME NOT NULL
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Токен — случайный UUID или подписанный JWT */
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** true = токен отозван (logout) */
    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ========================
    // Конструкторы
    // ========================
    public RefreshToken() {}

    public RefreshToken(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // ========================
    // Геттеры / Сеттеры
    // ========================
    public Long getId() { return id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
