package by.bsuir.filmcatalog.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Член съёмочной группы (crew) из TMDb credits.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCrewMemberDto {

    private String name;
    private String job;
    private String department;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
