package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * TMDB电影类型实体类
 */
@Data
public class Genre {
  @JsonProperty("id")
  private int id;

  @JsonProperty("name")
  private String name;
}