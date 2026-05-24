package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Ответ /api/v2.2/films/{id} — детали одного фильма.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpFilmDto {

    @JsonProperty("kinopoiskId")
    private Long kinopoiskId;

    @JsonProperty("nameRu")
    private String nameRu;

    @JsonProperty("nameEn")
    private String nameEn;

    @JsonProperty("nameOriginal")
    private String nameOriginal;

    @JsonProperty("year")
    private Integer year;

    @JsonProperty("description")
    private String description;

    @JsonProperty("ratingKinopoisk")
    private Double ratingKinopoisk;

    @JsonProperty("ratingImdb")
    private Double ratingImdb;

    @JsonProperty("filmLength")
    private Integer filmLength;  // в минутах

    @JsonProperty("posterUrl")
    private String posterUrl;

    @JsonProperty("posterUrlPreview")
    private String posterUrlPreview;

    @JsonProperty("coverUrl")
    private String coverUrl;

    @JsonProperty("type")
    private String type; // FILM, TV_SERIES, ...

    @JsonProperty("genres")
    private List<KpGenreDto> genres;

    @JsonProperty("countries")
    private List<KpCountryDto> countries;

    // ─── Getters ──────────────────────────────────────────────

    public Long getKinopoiskId() { return kinopoiskId; }
    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }
    public String getNameOriginal() { return nameOriginal; }
    public Integer getYear() { return year; }
    public String getDescription() { return description; }
    public Double getRatingKinopoisk() { return ratingKinopoisk; }
    public Double getRatingImdb() { return ratingImdb; }
    public Integer getFilmLength() { return filmLength; }
    public String getPosterUrl() { return posterUrl; }
    public String getPosterUrlPreview() { return posterUrlPreview; }
    public String getCoverUrl() { return coverUrl; }
    public String getType() { return type; }
    public List<KpGenreDto> getGenres() { return genres; }
    public List<KpCountryDto> getCountries() { return countries; }

    /** Удобный метод: лучшее доступное название */
    public String getBestName() {
        if (nameRu != null && !nameRu.isBlank()) return nameRu;
        if (nameEn != null && !nameEn.isBlank()) return nameEn;
        return nameOriginal;
    }

    /** Рейтинг: KP приоритет, потом IMDb */
    public Double getBestRating() {
        if (ratingKinopoisk != null && ratingKinopoisk > 0) return ratingKinopoisk;
        return ratingImdb;
    }
}
