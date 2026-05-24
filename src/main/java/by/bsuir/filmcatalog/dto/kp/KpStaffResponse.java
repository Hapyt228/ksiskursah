package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Элемент из GET /api/v1/staff?filmId={id} — актёры и съёмочная группа.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpStaffResponse {

    @JsonProperty("staffId")
    private Long staffId;

    @JsonProperty("nameRu")
    private String nameRu;

    @JsonProperty("nameEn")
    private String nameEn;

    @JsonProperty("professionKey")
    private String professionKey; // DIRECTOR, ACTOR, PRODUCER, ...

    public Long getStaffId() { return staffId; }
    public String getNameRu() { return nameRu; }
    public String getNameEn() { return nameEn; }
    public String getProfessionKey() { return professionKey; }

    public String getBestName() {
        if (nameRu != null && !nameRu.isBlank()) return nameRu;
        return nameEn;
    }

    public boolean isDirector() {
        return "DIRECTOR".equalsIgnoreCase(professionKey);
    }
}
