package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KpCountryDto {

    @JsonProperty("country")
    private String country;

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
