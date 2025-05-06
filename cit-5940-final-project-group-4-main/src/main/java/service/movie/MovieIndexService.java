package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.tmdbApi.TMDBApiService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 电影索引服务
 * 负责构建和维护电影的多种索引，提供快速查询功能
 */
@Slf4j
public class MovieIndexService {
    // 单例实例
    private static MovieIndexService instance;

    // 标题索引（前缀 -> 电影列表）
    private final Map<String, Set<Integer>> titlePrefixIndex = new ConcurrentHashMap<>();

    // ID索引（ID -> 电影）
    private final Map<Integer, Movie> idIndex = new ConcurrentHashMap<>();

    // 演员索引（演员ID -> 电影列表）
    private final Map<Integer, Set<Integer>> actorIndex = new ConcurrentHashMap<>();

    // 演员名称索引（演员名称 -> 演员ID）
    private final Map<String, Integer> actorNameIndex = new ConcurrentHashMap<>();

    // 导演索引（导演ID -> 电影列表）
    private final Map<Integer, Set<Integer>> directorIndex = new ConcurrentHashMap<>();

    // 导演名称索引（导演名称 -> 导演ID）
    private final Map<String, Integer> directorNameIndex = new ConcurrentHashMap<>();

    // 电影详情缓存
    private final Map<Integer, Movie> movieDetailsCache = new ConcurrentHashMap<>();

    // 电影演职人员缓存
    private final Map<Integer, MovieCredits> movieCreditsCache = new ConcurrentHashMap<>();

    /**
     * 私有构造函数
     */
    private MovieIndexService() {
    }

    /**
     * 获取单例实例
     */
    public static synchronized MovieIndexService getInstance() {
        if (instance == null) {
            instance = new MovieIndexService();
        }
        return instance;
    }

    /**
     * 初始化索引
     *
     * @param movies 电影列表
     */
    public void initializeIndexes(List<Movie> movies) {
        log.info("开始初始化电影索引，电影数量：{}", movies.size());

        for (Movie movie : movies) {
            // 添加到ID索引
            idIndex.put(movie.getId(), movie);

            // 添加到标题索引
            indexMovieTitle(movie);
        }

        log.info("电影索引初始化完成");
    }

    /**
     * 为电影标题建立索引
     */
    private void indexMovieTitle(Movie movie) {
        String title = movie
                .getTitle()
                .toLowerCase();
        for (int i = 1; i <= title.length(); i++) {
            String prefix = title.substring(0, i);
            titlePrefixIndex
                    .computeIfAbsent(prefix, k -> new HashSet<>())
                    .add(movie.getId());
        }
    }

    /**
     * 索引电影演职人员
     */
    public void indexMovieCredits(int movieId, MovieCredits credits) {
        // 缓存演职人员信息
        movieCreditsCache.put(movieId, credits);

        // 索引演员
        for (CastMember cast : credits.getCast()) {
            // 添加演员名称索引
            actorNameIndex.putIfAbsent(cast
                                               .getName()
                                               .toLowerCase(), cast.getId());

            // 添加演员-电影关联
            actorIndex
                    .computeIfAbsent(cast.getId(), k -> new HashSet<>())
                    .add(movieId);
        }

        // 索引导演
        for (CrewMember crew : credits.getCrew()) {
            if ("Director".equals(crew.getJob())) {
                // 添加导演名称索引
                directorNameIndex.putIfAbsent(crew
                                                      .getName()
                                                      .toLowerCase(), crew.getId());

                // 添加导演-电影关联
                directorIndex
                        .computeIfAbsent(crew.getId(), k -> new HashSet<>())
                        .add(movieId);
            }
        }
    }

    /**
     * 根据前缀搜索电影
     */
    public List<Movie> searchByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        prefix = prefix.toLowerCase();
        Set<Integer> movieIds = titlePrefixIndex.getOrDefault(prefix, Collections.emptySet());

        return movieIds
                .stream()
                .map(idIndex::get)
                .filter(Objects::nonNull)
                .sorted(Comparator
                                .comparing(Movie::getPopularity)
                                .reversed())
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取电影
     */
    public Movie getMovieById(int movieId) {
        // 先从详情缓存查询
        Movie movie = movieDetailsCache.get(movieId);
        if (movie != null) {
            return movie;
        }

        // 再从ID索引查询
        movie = idIndex.get(movieId);
        if (movie != null) {
            return movie;
        }

        // 都没有则调用API获取
        movie = TMDBApiService.getMovieDetails(movieId);
        if (movie != null) {
            // 缓存电影详情
            movieDetailsCache.put(movieId, movie);
            // 为电影标题建立索引
            indexMovieTitle(movie);
            // 缓存电影ID
            idIndex.put(movieId, movie);

            // 获取并索引电影演职人员
            MovieCredits credits = TMDBApiService.getMovieCredits(movieId);
            if (credits != null) {
                indexMovieCredits(movieId, credits);
            }
        }

        return movie;
    }

    /**
     * 获取电影演职人员
     */
    public MovieCredits getMovieCredits(int movieId) {
        // 先从缓存查询
        MovieCredits credits = movieCreditsCache.get(movieId);
        if (credits != null) {
            return credits;
        }

        // 缓存未命中则调用API
        credits = TMDBApiService.getMovieCredits(movieId);
        if (credits != null) {
            // 索引电影演职人员
            indexMovieCredits(movieId, credits);
        }

        return credits;
    }

    /**
     * 根据演员ID获取电影列表
     */
    public List<Movie> getMoviesByActor(int actorId) {
        Set<Integer> movieIds = actorIndex.getOrDefault(actorId, Collections.emptySet());

        return movieIds
                .stream()
                .map(this::getMovieById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据导演ID获取电影列表
     */
    public List<Movie> getMoviesByDirector(int directorId) {
        Set<Integer> movieIds = directorIndex.getOrDefault(directorId, Collections.emptySet());

        return movieIds
                .stream()
                .map(this::getMovieById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据演员名称获取演员ID
     */
    public Integer getActorIdByName(String name) {
        return actorNameIndex.get(name.toLowerCase());
    }

    /**
     * 根据导演名称获取导演ID
     */
    public Integer getDirectorIdByName(String name) {
        return directorNameIndex.get(name.toLowerCase());
    }

    /**
     * 设置电影演职人员信息（仅用于测试）
     *
     * @param movieId 电影ID
     * @param credits 演职人员信息
     */
    public void setMovieCreditsForTest(int movieId, MovieCredits credits) {
        movieCreditsCache.put(movieId, credits);
        indexMovieCredits(movieId, credits);
    }

    /**
     * 清空所有索引
     */
    public void clearIndexes() {
        titlePrefixIndex.clear();
        idIndex.clear();
        actorIndex.clear();
        actorNameIndex.clear();
        directorIndex.clear();
        directorNameIndex.clear();
        movieDetailsCache.clear();
        movieCreditsCache.clear();
    }
}
