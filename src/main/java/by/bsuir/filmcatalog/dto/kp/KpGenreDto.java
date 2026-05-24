package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KpGenreDto {

    @JsonProperty("genre")
    private String genre;

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
}
