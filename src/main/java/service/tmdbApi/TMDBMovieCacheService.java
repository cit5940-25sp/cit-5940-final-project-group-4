package service.tmdbApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import model.tmdb.MovieList;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TMDB电影缓存服务
 * 负责多线程获取电影数据并进行本地缓存
 */
@Slf4j
public class TMDBMovieCacheService {
    private static final int CACHE_DURATION_HOURS = 24; // 缓存更新周期（小时）
    private static final int THREAD_POOL_SIZE = 5; // 线程池大小
    private static final int BATCH_SIZE = 100; // 每个线程处理的电影数量
    private static final int PAGE_SIZE = 20; // TMDB API默认每页数量
    private static final int MAX_RETRIES = 3; // 最大重试次数
    private static final long RETRY_DELAY = 1000; // 重试延迟（毫秒）
    private static final long REQUEST_DELAY = 200; // 请求间隔（毫秒）

    private static final ExecutorService EXECUTOR_SERVICE =
            Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String cache = "cache";
    private static String cacheFile = cache + "/popular_movies.json";
    private static String lastUpdateFile = cache + "/last_update.txt";

    // 测试模式标志
    private static boolean testMode = false;
    // 禁用缓存标志
    private static boolean disableCache = false;

    /**
     * 设置缓存目录（用于测试）
     */
    public static void setCache(String dir) {
        cache = dir;
        cacheFile = cache + "/popular_movies.json";
        lastUpdateFile = cache + "/last_update.txt";
        // 设置为测试模式
        testMode = true;
    }

    /**
     * 设置测试模式
     *
     * @param disableCaching 是否禁用缓存（直接API访问）
     */
    public static void setTestMode(boolean disableCaching) {
        disableCache = disableCaching;
    }

    /**
     * 获取指定数量的热门电影（支持缓存）
     *
     * @param count 需要获取的电影数量
     * @return 电影列表
     */
    public static List<Movie> getPopularMovies(int count) {
        // 测试模式下直接从API获取数据
        if (testMode) {
            log.info("测试模式：直接从API获取数据");
            return getPopularMoviesFromApi(count);
        }

        // 禁用缓存时直接从API获取
        if (disableCache) {
            log.info("禁用缓存：直接从API获取数据");
            return getPopularMoviesFromApi(count);
        }

        // 检查缓存目录
        createCacheDirectoryIfNeeded();

        // 检查是否需要更新缓存
        if (shouldUpdateCache()) {
            return updateMovieCache(count);
        }

        // 从缓存读取
        List<Movie> movies = readFromCache();
        // 如果缓存的电影数量不够，重新获取
        if (movies.size() < count) {
            return updateMovieCache(count);
        }
        // 返回指定数量的电影
        return movies.size() > count ? movies.subList(0, count) : movies;
    }

    /**
     * 创建缓存目录（如果不存在）
     */
    private static void createCacheDirectoryIfNeeded() {
        try {
            Files.createDirectories(Paths.get(cache));
        } catch (Exception e) {
            log.error("创建缓存目录失败", e);
        }
    }

    /**
     * 检查是否需要更新缓存
     */
    private static boolean shouldUpdateCache() {
        Path lastUpdatePath = Paths.get(lastUpdateFile);
        if (!Files.exists(lastUpdatePath)) {
            return true;
        }

        try {
            String lastUpdateStr = new String(Files.readAllBytes(lastUpdatePath),
                                              StandardCharsets.UTF_8);
            LocalDateTime lastUpdate = LocalDateTime.parse(lastUpdateStr);
            return LocalDateTime
                    .now()
                    .minusHours(CACHE_DURATION_HOURS)
                    .isAfter(lastUpdate);
        } catch (Exception e) {
            log.error("读取上次更新时间失败", e);
            return true;
        }
    }

    /**
     * 更新电影缓存
     */
    private static List<Movie> updateMovieCache(int count) {
        log.info("开始更新电影缓存...");
        List<Movie> movies = fetchMoviesWithThreadPool(count);

        if (!movies.isEmpty()) {
            // 保存到缓存
            try {
                String json = OBJECT_MAPPER.writeValueAsString(movies);
                Files.write(Paths.get(cacheFile), json.getBytes(StandardCharsets.UTF_8));
                // 更新最后更新时间
                Files.write(Paths.get(lastUpdateFile), LocalDateTime
                        .now()
                        .toString()
                        .getBytes(StandardCharsets.UTF_8));
                log.info("电影缓存更新成功，共{}部电影", movies.size());
            } catch (Exception e) {
                log.error("保存缓存文件失败", e);
            }
        }

        return movies;
    }

    /**
     * 从缓存读取电影数据
     */
    private static List<Movie> readFromCache() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(cacheFile)),
                                     StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Movie>>() {
            });
        } catch (Exception e) {
            log.error("读取缓存文件失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 使用线程池获取电影数据
     */
    private static List<Movie> fetchMoviesWithThreadPool(int totalCount) {
        int batchCount = (totalCount + BATCH_SIZE - 1) / BATCH_SIZE;
        List<Future<List<Movie>>> futures = new ArrayList<>();
        AtomicInteger processedCount = new AtomicInteger(0);

        // 提交任务到线程池，每个线程处理不同的批次
        for (int i = 0; i < batchCount; i++) {
            final int batchIndex = i;
            final int batchSize = Math.min(BATCH_SIZE, totalCount - i * BATCH_SIZE);
            futures.add(EXECUTOR_SERVICE.submit(() -> {
                List<Movie> batchMovies = getPopularMoviesFromApi(batchSize);
                if (batchMovies != null && !batchMovies.isEmpty()) {
                    int current = processedCount.addAndGet(batchMovies.size());
                    log.info("线程[{}]已获取第{}批数据，当前共{}部电影", Thread
                            .currentThread()
                            .getId(), batchIndex + 1, current);
                }
                return batchMovies != null ? batchMovies : new ArrayList<>();
            }));
        }

        // 收集结果
        List<Movie> allMovies = new ArrayList<>();
        for (Future<List<Movie>> future : futures) {
            try {
                List<Movie> movies = future.get(5, TimeUnit.MINUTES);
                if (movies != null) {
                    allMovies.addAll(movies);
                }
            } catch (Exception e) {
                log.error("获取电影数据失败", e);
            }
        }

        // 按人气排序并限制数量
        allMovies.sort((m1, m2) -> Double.compare(m2.getPopularity(), m1.getPopularity()));
        return allMovies.size() > totalCount ? allMovies.subList(0, totalCount) : allMovies;
    }

    /**
     * 从API获取指定数量的最受欢迎电影
     *
     * @param count 需要获取的电影数量
     * @return 电影列表
     */
    private static List<Movie> getPopularMoviesFromApi(int count) {
        List<Movie> allMovies = new ArrayList<>();
        int totalPages = (count + PAGE_SIZE - 1) / PAGE_SIZE; // 向上取整
        int currentPage = 1;

        while (currentPage <= totalPages && allMovies.size() < count) {
            MovieList movieList = null;
            // 添加重试机制
            for (int retry = 0; retry < MAX_RETRIES; retry++) {
                try {
                    movieList = TMDBApiService.discoverMovies(currentPage, "popularity.desc");
                    if (movieList != null && movieList.getResults() != null && !movieList
                            .getResults()
                            .isEmpty()) {
                        break;
                    }
                    if (retry < MAX_RETRIES - 1) {
                        log.info("第{}次重试获取第{}页数据...", retry + 1, currentPage);
                        Thread.sleep(RETRY_DELAY);
                    }
                } catch (Exception e) {
                    log.error("第{}次尝试获取第{}页数据失败", retry + 1, currentPage, e);
                    if (retry < MAX_RETRIES - 1) {
                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch (InterruptedException ie) {
                            Thread
                                    .currentThread()
                                    .interrupt();
                            break;
                        }
                    }
                }
            }

            if (movieList == null || movieList.getResults() == null || movieList
                    .getResults()
                    .isEmpty()) {
                log.error("获取第{}页电影数据失败，已重试{}次", currentPage, MAX_RETRIES);
                break;
            }

            // 确保不重复添加电影
            for (Movie movie : movieList.getResults()) {
                if (!allMovies.contains(movie)) {
                    allMovies.add(movie);
                    if (allMovies.size() >= count) {
                        break;
                    }
                }
            }

            log.info("已获取第{}页电影数据，当前共{}部电影", currentPage, allMovies.size());

            // 如果已经获取足够的电影或者没有更多数据，则退出循环
            if (allMovies.size() >= count || currentPage >= movieList.getTotalPages()) {
                break;
            }

            currentPage++;

            // 为了避免触发API限流，添加短暂延时
            try {
                Thread.sleep(REQUEST_DELAY);
            } catch (InterruptedException e) {
                Thread
                        .currentThread()
                        .interrupt();
                break;
            }
        }

        // 如果获取的电影超过指定数量，只返回指定数量
        return allMovies.size() > count ? allMovies.subList(0, count) : allMovies;
    }
}
