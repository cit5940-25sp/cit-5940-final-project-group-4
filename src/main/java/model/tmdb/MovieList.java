package model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * TMDB电影列表实体类
 */
@Data
public class MovieList {
    @JsonProperty("page")
    private int page;

    @JsonProperty("results")
    private List<Movie> results;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_results")
    private int totalResults;
}
