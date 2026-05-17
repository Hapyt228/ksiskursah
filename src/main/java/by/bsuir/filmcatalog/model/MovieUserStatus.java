package by.bsuir.filmcatalog.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Статус фильма у конкретного пользователя.
 * Один пользователь — один статус для одного фильма.
 *
 * Структура таблицы:
 *   id          BIGINT PK
 *   user_id     BIGINT FK -> users.id
 *   film_id     BIGINT FK -> films.id
 *   status      TEXT NOT NULL   (watching, planned, dropped, completed, favourite, rewatching, postponed)
 *   updated_at  DATETIME NOT NULL
 */
@Entity
@Table(name = "movie_user_status",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "film_id"}))
public class MovieUserStatus {

    /**
     * Возможные статусы с весовыми коэффициентами для рекомендаций:
     *  FAVOURITE   = 5  (самый высокий вес)
     *  REWATCHING  = 5
     *  WATCHING    = 3
     *  PLANNED     = 3
     *  COMPLETED   = 3
     *  POSTPONED   = 1
     *  DROPPED     = 0  (игнорируется в рекомендациях)
     */
    public enum WatchStatus {
        WATCHING,    // Смотрю
        PLANNED,     // Запланировано
        DROPPED,     // Брошено  (не учитывается в рекомендациях)
        COMPLETED,   // Просмотрено
        FAVOURITE,   // Любимое
        REWATCHING,  // Пересматриваю
        POSTPONED    // Отложено
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchStatus status;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ========================
    // Конструкторы
    // ========================
    public MovieUserStatus() {}

    public MovieUserStatus(User user, Film film, WatchStatus status) {
        this.user = user;
        this.film = film;
        this.status = status;
    }

    // ========================
    // Геттеры / Сеттеры
    // ========================
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Film getFilm() { return film; }
    public void setFilm(Film film) { this.film = film; }
    public WatchStatus getStatus() { return status; }
    public void setStatus(WatchStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
