package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Жанр из TMDb API.
 * Используется в ответах /genre/movie/list и /movie/{id} (поле genres).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbGenreDto {

    private Integer id;
    private String name;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
