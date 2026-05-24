package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Страница результатов поиска персон от TMDb.
 */
public class TmdbPersonPageResponse {

    @JsonProperty("results")
    private List<TmdbPersonDto> results;

    @JsonProperty("total_results")
    private int totalResults;

    // ========================
    // Геттеры и сеттеры
    // ========================

    public List<TmdbPersonDto> getResults() { return results; }
    public void setResults(List<TmdbPersonDto> results) { this.results = results; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }
}
