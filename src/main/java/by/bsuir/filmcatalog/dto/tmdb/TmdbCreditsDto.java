package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Credits из TMDb (режиссёры и актёры).
 * Используется в append_to_response=credits при запросе деталей фильма.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCreditsDto {

    private List<TmdbCrewMemberDto> crew;

    public List<TmdbCrewMemberDto> getCrew() { return crew; }
    public void setCrew(List<TmdbCrewMemberDto> crew) { this.crew = crew; }

    /**
     * Ищет режиссёра в списке crew (job = "Director").
     */
    public String findDirector() {
        if (crew == null) return null;
        return crew.stream()
                .filter(m -> "Director".equals(m.getJob()))
                .map(TmdbCrewMemberDto::getName)
                .findFirst()
                .orElse(null);
    }
}
