package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Элемент из поиска персон /api/v1/persons
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpPersonItem {

    @JsonProperty("kinopoiskId")
    private Long kinopoiskId;

    @JsonProperty("nameRu")
    private String nameRu;

    @JsonProperty("nameEn")
    private String nameEn;

    @JsonProperty("sex")
    private String sex;

    @JsonProperty("posterUrl")
    private String posterUrl;

    public Long getKinopoiskId() { return kinopoiskId; }
    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }

    public String getBestName() {
        if (nameRu != null && !nameRu.isBlank()) return nameRu;
        return nameEn;
    }
}
