package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 电影剧组成员模型
 */
@Data
@EqualsAndHashCode(of = "id")
public class CrewMember {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("job")
    private String job;

    @JsonProperty("department")
    private String department;

    @JsonProperty("profile_path")
    private String profilePath;

    @JsonProperty("gender")
    private int gender;
}
