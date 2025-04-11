package service.tmdbApi;

import config.AppConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import model.tmdb.Genre;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import model.tmdb.MovieList;
import utils.HttpUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TMDB API服务
 * 只封装TMDB的原始API请求
 */
@Slf4j
public class TMDBApiService {
    private static final AppConfig CONFIG = AppConfig.getInstance();

    // 测试模式标志
    private static boolean testMode = false;
    // 测试数据
    private static MovieList testMovieList;
    private static Movie testMovieDetails;
    private static MovieCredits testMovieCredits;

    @Getter
    private static String baseUrl = CONFIG.getProperty("tmdb.api.base-url", "https://api" +
            ".themoviedb.org/3");
    private static String discoverMovieUrl = baseUrl + "/discover/movie";
    private static String searchMovieUrl = baseUrl + "/search/movie";
    private static String movieDetailsUrl = baseUrl + "/movie/%d";
    private static String movieCreditsUrl = baseUrl + "/movie/%d/credits";
    private static String genresUrl = baseUrl + "/genre/movie/list";

    /**
     * 设置基础URL（仅用于测试）
     */
    public static void setBaseUrl(String url) {
        baseUrl = url;
        discoverMovieUrl = baseUrl + "/discover/movie";
        searchMovieUrl = baseUrl + "/search/movie";
        movieDetailsUrl = baseUrl + "/movie/%d";
        movieCreditsUrl = baseUrl + "/movie/%d/credits";
        genresUrl = baseUrl + "/genre/movie/list";
    }

    /**
     * 设置测试模式
     *
     * @param isTestMode 是否为测试模式
     * @param movieList  测试用电影列表数据
     */
    public static void setTestMode(boolean isTestMode, MovieList movieList) {
        testMode = isTestMode;
        testMovieList = movieList;
    }

    /**
     * 设置测试详情数据
     *
     * @param movieDetails 电影详情
     * @param movieCredits 电影演职人员
     */
    public static void setTestDetailData(Movie movieDetails, MovieCredits movieCredits) {
        testMovieDetails = movieDetails;
        testMovieCredits = movieCredits;
    }

    /**
     * 发现电影API
     *
     * @param page   页码
     * @param sortBy 排序方式
     * @return 电影列表
     */
    public static MovieList discoverMovies(int page, String sortBy) {
        // 测试模式下直接返回测试数据
        if (testMode && testMovieList != null) {
            log.info("测试模式：返回测试数据");
            return testMovieList;
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            // 构建带参数的URL
            String url =
                    String.format("%s?include_adult=false&include_video=false&language=%s" +
                                          "&page=%d&sort_by=%s&api_key=%s", discoverMovieUrl,
                                  CONFIG.getProperty("tmdb.api.language", "en-US"), page, sortBy,
                                  CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            return HttpUtil.fromJson(response, MovieList.class);
        } catch (Exception e) {
            log.error("获取电影列表异常", e);
            return null;
        }
    }

    /**
     * 搜索电影API
     *
     * @param query 搜索关键词
     * @param page  页码
     * @return 电影列表
     */
    public static List<Movie> searchMovies(String query, int page) {
        // 测试模式下直接返回测试数据
        if (testMode && testMovieList != null) {
            log.info("测试模式：返回搜索测试数据");
            return testMovieList.getResults();
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            // 构建带参数的URL
            String url = String.format("%s?query=%s&include_adult=false&language=%s&page=%d" +
                                               "&api_key=%s", searchMovieUrl,
                                       HttpUtil.urlEncode(query), CONFIG.getProperty("tmdb.api.language", "en-US"), page, CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            MovieList movieList = HttpUtil.fromJson(response, MovieList.class);
            return movieList != null ? movieList.getResults() : Collections.emptyList();
        } catch (Exception e) {
            log.error("搜索电影异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取电影详情API
     *
     * @param movieId 电影ID
     * @return 电影详情
     */
    public static Movie getMovieDetails(int movieId) {
        // 测试模式下直接返回测试数据
        if (testMode && testMovieDetails != null) {
            log.info("测试模式：返回电影详情测试数据");
            return testMovieDetails;
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            // 构建URL
            String url = String.format(movieDetailsUrl + "?language=%s&api_key=%s", movieId,
                                       CONFIG.getProperty("tmdb.api.language", "en-US"),
                                       CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            return HttpUtil.fromJson(response, Movie.class);
        } catch (Exception e) {
            log.error("获取电影详情异常", e);
            return null;
        }
    }

    /**
     * 获取电影演职人员API
     *
     * @param movieId 电影ID
     * @return 电影演职人员
     */
    public static MovieCredits getMovieCredits(int movieId) {
        // 测试模式下直接返回测试数据
        if (testMode && testMovieCredits != null) {
            log.info("测试模式：返回电影演职人员测试数据");
            return testMovieCredits;
        }

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            // 构建URL
            String url = String.format(movieCreditsUrl + "?language=%s&api_key=%s", movieId,
                                       CONFIG.getProperty("tmdb.api.language", "en-US"),
                                       CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            return HttpUtil.fromJson(response, MovieCredits.class);
        } catch (Exception e) {
            log.error("获取电影演职人员异常", e);
            return null;
        }
    }

    /**
     * 获取电影类型列表API
     *
     * @return 电影类型列表
     */
    public static List<Genre> getMovieGenres() {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");

            // 构建URL
            String url = String.format("%s?language=%s&api_key=%s", genresUrl,
                                       CONFIG.getProperty("tmdb.api.language", "en-US"),
                                       CONFIG.getProperty("tmdb.api.key"));

            String response = HttpUtil.get(url, headers);
            GenreList genreList = HttpUtil.fromJson(response, GenreList.class);
            return genreList != null ? genreList.getGenres() : Collections.emptyList();
        } catch (Exception e) {
            log.error("获取电影类型列表异常", e);
            return Collections.emptyList();
        }
    }

    @Getter
    private static class GenreList {
        private List<Genre> genres;
    }
}
