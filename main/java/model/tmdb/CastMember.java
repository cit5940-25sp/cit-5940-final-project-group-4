package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 电影演员模型
 */
@Data
@EqualsAndHashCode(of = "id")
public class CastMember {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("character")
    private String character;

    @JsonProperty("order")
    private int order;

    @JsonProperty("profile_path")
    private String profilePath;

    @JsonProperty("gender")
    private int gender;
}
