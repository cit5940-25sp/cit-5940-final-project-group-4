package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * TMDB电影类型实体类
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Genre {
  @JsonProperty("id")
  private int id;

  @JsonProperty("name")
  private String name;
}