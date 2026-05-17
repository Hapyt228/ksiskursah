package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO для ответа TMDb API — один фильм.
 * Используется как для /movie/{id} (детали), так и для объектов в списках.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieDto {

    private Long id;

    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    @JsonProperty("release_date")
    private String releaseDate;           // "YYYY-MM-DD"

    private String overview;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("poster_path")
    private String posterPath;            // "/abc123.jpg" — нужно добавить базовый URL

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("genre_ids")
    private List<Integer> genreIds;       // В списках (popular, search)

    private List<TmdbGenreDto> genres;    // В детальном запросе /movie/{id}

    private Integer runtime;              // Длительность в минутах

    @JsonProperty("production_countries")
    private List<TmdbCountryDto> productionCountries;

    @JsonProperty("credits")
    private TmdbCreditsDto credits;       // Режиссёр — из credits.crew

    // ========================
    // Геттеры и сеттеры
    // ========================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }

    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }

    public String getBackdropPath() { return backdropPath; }
    public void setBackdropPath(String backdropPath) { this.backdropPath = backdropPath; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }

    public List<Integer> getGenreIds() { return genreIds; }
    public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds; }

    public List<TmdbGenreDto> getGenres() { return genres; }
    public void setGenres(List<TmdbGenreDto> genres) { this.genres = genres; }

    public Integer getRuntime() { return runtime; }
    public void setRuntime(Integer runtime) { this.runtime = runtime; }

    public List<TmdbCountryDto> getProductionCountries() { return productionCountries; }
    public void setProductionCountries(List<TmdbCountryDto> productionCountries) {
        this.productionCountries = productionCountries;
    }

    public TmdbCreditsDto getCredits() { return credits; }
    public void setCredits(TmdbCreditsDto credits) { this.credits = credits; }

    /** Извлекает год из releaseDate ("2010-07-16" → 2010) */
    public Integer getYear() {
        if (releaseDate != null && releaseDate.length() >= 4) {
            try { return Integer.parseInt(releaseDate.substring(0, 4)); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
