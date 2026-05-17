package by.bsuir.filmcatalog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * Пользовательская оценка фильма (1–5 звёзд).
 * Один пользователь — одна оценка для одного фильма.
 *
 * Структура таблицы:
 *   id         BIGINT PK
 *   user_id    BIGINT FK -> users.id
 *   film_id    BIGINT FK -> films.id
 *   stars      INTEGER NOT NULL (1-5)
 *   rated_at   DATETIME NOT NULL
 */
@Entity
@Table(name = "movie_ratings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "film_id"}))
public class MovieRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    /** Оценка от 1 до 5 звёзд */
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer stars;

    @Column(name = "rated_at", nullable = false)
    private LocalDateTime ratedAt = LocalDateTime.now();

    // ========================
    // Конструкторы
    // ========================
    public MovieRating() {}

    public MovieRating(User user, Film film, Integer stars) {
        this.user = user;
        this.film = film;
        this.stars = stars;
    }

    // ========================
    // Геттеры / Сеттеры
    // ========================
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Film getFilm() { return film; }
    public void setFilm(Film film) { this.film = film; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) {
        this.stars = stars;
        this.ratedAt = LocalDateTime.now();
    }
    public LocalDateTime getRatedAt() { return ratedAt; }
}
