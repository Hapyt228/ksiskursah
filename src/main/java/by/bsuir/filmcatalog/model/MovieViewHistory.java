package by.bsuir.filmcatalog.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * История просмотров фильмов.
 * Фиксирует каждый заход пользователя на страницу фильма.
 *
 * Структура таблицы:
 *   id          BIGINT PK
 *   user_id     BIGINT FK -> users.id
 *   film_id     BIGINT FK -> films.id
 *   viewed_at   DATETIME NOT NULL
 */
@Entity
@Table(name = "movie_view_history",
       indexes = {
           @Index(name = "idx_view_user", columnList = "user_id"),
           @Index(name = "idx_view_film", columnList = "film_id")
       })
public class MovieViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();

    // ========================
    // Конструкторы
    // ========================
    public MovieViewHistory() {}

    public MovieViewHistory(User user, Film film) {
        this.user = user;
        this.film = film;
    }

    // ========================
    // Геттеры / Сеттеры
    // ========================
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Film getFilm() { return film; }
    public void setFilm(Film film) { this.film = film; }
    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}
