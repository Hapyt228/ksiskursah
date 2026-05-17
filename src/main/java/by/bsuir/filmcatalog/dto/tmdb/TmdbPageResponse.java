package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Обёртка для страничных ответов TMDb (popular, search, recommendations и т.д.).
 * TMDb возвращает: { "page": 1, "results": [...], "total_pages": 500, "total_results": 10000 }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbPageResponse {

    private Integer page;

    private List<TmdbMovieDto> results;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("total_results")
    private Integer totalResults;

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public List<TmdbMovieDto> getResults() { return results; }
    public void setResults(List<TmdbMovieDto> results) { this.results = results; }

    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

    public Integer getTotalResults() { return totalResults; }
    public void setTotalResults(Integer totalResults) { this.totalResults = totalResults; }
}
