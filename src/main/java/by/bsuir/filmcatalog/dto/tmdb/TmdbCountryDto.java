package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Страна производства из TMDb.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCountryDto {

    @JsonProperty("iso_3166_1")
    private String iso;

    private String name;

    public String getIso() { return iso; }
    public void setIso(String iso) { this.iso = iso; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
