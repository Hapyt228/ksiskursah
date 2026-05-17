package by.bsuir.filmcatalog.dto;

import jakarta.validation.constraints.*;

/**
 * DTO (Data Transfer Object) для передачи данных о фильме через REST API.
 * Используется как для входящих запросов (создание/обновление),
 * так и для исходящих ответов.
 */
public class FilmDto {

    private Long id;

    /** ID фильма в TMDb (null для локальных фильмов) */
    private Long tmdbId;

    @NotBlank(message = "Название фильма обязательно")
    @Size(max = 255)
    private String title;

    private String originalTitle;

    @NotNull(message = "Год выхода обязателен")
    @Min(value = 1888)
    @Max(value = 2100)
    private Integer year;

    @NotBlank(message = "Жанр обязателен")
    private String genre;

    @NotBlank(message = "Режиссёр обязателен")
    private String director;

    private String country;

    private String description;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private Double rating;

    @Min(1)
    private Integer durationMinutes;

    private String posterUrl;

    private String language;

    private String tags;

    // ========================
    // Конструкторы
    // ========================

    public FilmDto() {}

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
}
