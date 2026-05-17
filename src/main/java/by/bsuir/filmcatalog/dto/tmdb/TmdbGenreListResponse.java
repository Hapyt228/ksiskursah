package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Ответ TMDb на запрос /genre/movie/list.
 * Формат: { "genres": [ {"id": 28, "name": "Боевик"}, ... ] }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbGenreListResponse {

    private List<TmdbGenreDto> genres;

    public List<TmdbGenreDto> getGenres() { return genres; }
    public void setGenres(List<TmdbGenreDto> genres) { this.genres = genres; }
}
