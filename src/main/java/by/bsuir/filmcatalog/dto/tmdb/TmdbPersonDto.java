package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Персона из TMDb /search/person.
 * Используется для поиска режиссёра по имени.
 */
public class TmdbPersonDto {

    private Long id;

    private String name;

    /** Известная роль (Acting, Directing и т.д.) */
    @JsonProperty("known_for_department")
    private String knownForDepartment;

    // ========================
    // Геттеры и сеттеры
    // ========================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKnownForDepartment() { return knownForDepartment; }
    public void setKnownForDepartment(String knownForDepartment) {
        this.knownForDepartment = knownForDepartment;
    }
}
