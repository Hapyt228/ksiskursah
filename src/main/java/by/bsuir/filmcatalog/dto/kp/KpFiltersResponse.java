package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Ответ GET /api/v2.2/films/filters — список жанров и стран.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpFiltersResponse {

    @JsonProperty("genres")
    private List<KpGenreFilter> genres;

    @JsonProperty("countries")
    private List<KpCountryFilter> countries;

    public List<KpGenreFilter> getGenres() { return genres; }
    public List<KpCountryFilter> getCountries() { return countries; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KpGenreFilter {
        @JsonProperty("id")
        private Integer id;
        @JsonProperty("genre")
        private String genre;

        public Integer getId() { return id; }
        public String getGenre() { return genre; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KpCountryFilter {
        @JsonProperty("id")
        private Integer id;
        @JsonProperty("country")
        private String country;

        public Integer getId() { return id; }
        public String getCountry() { return country; }
    }
}
