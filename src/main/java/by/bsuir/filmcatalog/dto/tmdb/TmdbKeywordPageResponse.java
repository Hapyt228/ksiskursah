package by.bsuir.filmcatalog.dto.tmdb;

import java.util.List;

/**
 * Страница результатов поиска ключевых слов от TMDb.
 */
public class TmdbKeywordPageResponse {

    private List<TmdbKeywordDto> results;

    public List<TmdbKeywordDto> getResults() { return results; }
    public void setResults(List<TmdbKeywordDto> results) { this.results = results; }
}
