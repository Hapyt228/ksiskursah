package by.bsuir.filmcatalog.dto.kp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Ответ GET /api/v1/persons?name=... — поиск персоны по имени.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpPersonResponse {

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("items")
    private List<KpPersonItem> items;

    public Integer getTotal() { return total; }
    public List<KpPersonItem> getItems() { return items; }
}
