package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Элемент из списка фильмов (поиск, топ, похожие).
 * Отличается от KpFilmDto — меньше полей, нет credits.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpFilmItem {

    @JsonProperty("filmId")
    private Long filmId;

    // В /api/v2.2/films используется kinopoiskId
    @JsonProperty("kinopoiskId")
    private Long kinopoiskId;

    @JsonProperty("nameRu")
    private String nameRu;

    @JsonProperty("nameEn")
    private String nameEn;

    @JsonProperty("nameOriginal")
    private String nameOriginal;

    @JsonProperty("year")
    private Object year; // может быть строкой "2023" или числом

    @JsonProperty("description")
    private String description;

    @JsonProperty("rating")
    private Object rating; // может быть строкой "7.5" или числом

    @JsonProperty("ratingKinopoisk")
    private Double ratingKinopoisk;

    @JsonProperty("ratingImdb")
    private Double ratingImdb;

    @JsonProperty("posterUrl")
    private String posterUrl;

    @JsonProperty("posterUrlPreview")
    private String posterUrlPreview;

    @JsonProperty("type")
    private String type;

    @JsonProperty("genres")
    private List<KpGenreDto> genres;

    @JsonProperty("countries")
    private List<KpCountryDto> countries;

    // ─── Getters ──────────────────────────────────────────────

    /** Возвращает KP id независимо от поля (filmId или kinopoiskId) */
    public Long getId() {
        if (kinopoiskId != null) return kinopoiskId;
        return filmId;
    }

    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }
    public String getNameOriginal() { return nameOriginal; }
    public String getPosterUrl() { return posterUrl; }
    public String getPosterUrlPreview() { return posterUrlPreview; }
    public String getType() { return type; }
    public List<KpGenreDto> getGenres() { return genres; }
    public List<KpCountryDto> getCountries() { return countries; }

    public String getBestName() {
        if (nameRu != null && !nameRu.isBlank()) return nameRu;
        if (nameEn != null && !nameEn.isBlank()) return nameEn;
        return nameOriginal;
    }

    public Integer getYearInt() {
        if (year == null) return null;
        try { return Integer.parseInt(year.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    public Double getBestRating() {
        if (ratingKinopoisk != null && ratingKinopoisk > 0) return ratingKinopoisk;
        if (ratingImdb != null && ratingImdb > 0) return ratingImdb;
        if (rating != null) {
            try { return Double.parseDouble(rating.toString()); }
            catch (NumberFormatException e) { /* ignore */ }
        }
        return null;
    }
}
