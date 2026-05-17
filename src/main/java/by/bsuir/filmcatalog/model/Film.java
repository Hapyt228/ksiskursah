package by.bsuir.filmcatalog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Сущность «Фильм» — основная модель предметной области.
 * Хранит информацию о кинофильме в базе данных SQLite.
 */
@Entity
@Table(name = "films")
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID фильма в TMDb.
     * null = фильм был добавлен вручную (не через TMDb).
     * Не null = фильм получен из TMDb и сохранён локально для связи с историей просмотров.
     */
    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    /** Название фильма */
    @NotBlank(message = "Название фильма обязательно")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    @Column(nullable = false)
    private String title;

    /** Оригинальное название */
    @Column(name = "original_title")
    private String originalTitle;

    /** Год выхода */
    @Min(value = 1888, message = "Год выхода не может быть раньше 1888")
    @Max(value = 2100, message = "Год выхода некорректен")
    @Column(nullable = false)
    private Integer year;

    /** Жанр */
    @NotBlank(message = "Жанр обязателен")
    @Column(nullable = false)
    private String genre;

    /** Режиссёр */
    @NotBlank(message = "Режиссёр обязателен")
    @Column(nullable = false)
    private String director;

    /** Страна производства */
    private String country;

    /** Описание / синопсис */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Рейтинг (0.0 — 10.0) */
    @DecimalMin(value = "0.0", message = "Рейтинг не может быть меньше 0")
    @DecimalMax(value = "10.0", message = "Рейтинг не может быть больше 10")
    private Double rating;

    /** Продолжительность в минутах */
    @Min(value = 1, message = "Длительность должна быть положительной")
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /** URL постера */
    @Column(name = "poster_url")
    private String posterUrl;

    /** Язык оригинала */
    private String language;

    /** Теги/ключевые слова (хранятся как строка, разделённая запятыми) */
    @Column(columnDefinition = "TEXT")
    private String tags;

    // ========================
    // Конструкторы
    // ========================

    public Film() {}

    public Film(String title, String originalTitle, Integer year, String genre,
                String director, String country, String description,
                Double rating, Integer durationMinutes, String posterUrl,
                String language, String tags) {
        this.title = title;
        this.originalTitle = originalTitle;
        this.year = year;
        this.genre = genre;
        this.director = director;
        this.country = country;
        this.description = description;
        this.rating = rating;
        this.durationMinutes = durationMinutes;
        this.posterUrl = posterUrl;
        this.language = language;
        this.tags = tags;
    }

    // ========================
    // Геттеры и сеттеры
    // ========================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    @Override
    public String toString() {
        return "Film{id=" + id + ", title='" + title + "', year=" + year + ", genre='" + genre + "'}";
    }
}
