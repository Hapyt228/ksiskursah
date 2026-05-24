package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Ответ GET /api/v2.2/films/{id}/similars
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpSimilarsResponse {

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("items")
    private List<KpFilmItem> items;

    public Integer getTotal() { return total; }
    public List<KpFilmItem> getItems() { return items != null ? items : java.util.Collections.emptyList(); }
}
