package service.tmdbApi;

import config.AppConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;

import java.util.List;

/**
 * TMDB电影服务类
 * 业务层服务，负责对接缓存服务和API服务
 */
@Slf4j
public class TMDBMovieService {
    public static final int MAX_MOVIES = 10;
    private static final AppConfig CONFIG = AppConfig.getInstance();
    @Getter
    private static String baseUrl = CONFIG.getProperty("tmdb.api.base-url", "https://api" +
            ".themoviedb.org/3");

    /**
     * 设置基础URL（仅用于测试）
     */
    public static void setBaseUrl(String url) {
        baseUrl = url;
        TMDBApiService.setBaseUrl(url);
    }

    /**
     * 获取前5000个最受欢迎的电影
     *
     * @return 电影列表
     */
    public static List<Movie> getTop5000PopularMovies() {
        return TMDBMovieCacheService.getPopularMovies(MAX_MOVIES);
    }

    /**
     * 获取指定数量的最受欢迎电影（仅用于测试）
     *
     * @param count 需要获取的电影数量
     * @return 电影列表
     */
    public static List<Movie> getPopularMovies(int count) {
        return TMDBMovieCacheService.getPopularMovies(count);
    }
}
