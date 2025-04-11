package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 电影演职人员信息
 */
@Data
public class MovieCredits {
    @JsonProperty("id")
    private int id;

    @JsonProperty("cast")
    private List<CastMember> cast;

    @JsonProperty("crew")
    private List<CrewMember> crew;
}
