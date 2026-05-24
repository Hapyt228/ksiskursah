package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Ответ /api/v2.1/films/search-by-keyword и /api/v2.2/films/top
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpSearchResponse {

    @JsonProperty("keyword")
    private String keyword;

    @JsonProperty("pagesCount")
    private Integer pagesCount;

    @JsonProperty("searchFilmsCountResult")
    private Integer searchFilmsCountResult;

    @JsonProperty("films")
    private List<KpFilmItem> films;

    // top endpoint uses "films" too but sometimes "items"
    @JsonProperty("items")
    private List<KpFilmItem> items;

    public Integer getPagesCount() { return pagesCount; }
    public Integer getSearchFilmsCountResult() { return searchFilmsCountResult; }

    public List<KpFilmItem> getResults() {
        if (films != null && !films.isEmpty()) return films;
        if (items != null) return items;
        return java.util.Collections.emptyList();
    }
}
