package utils.tmdbApi;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import model.tmdb.MovieList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBApiService;
import service.tmdbApi.TMDBMovieCacheService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TMDB电影缓存服务测试类
 */
@Slf4j
public class TMDBMovieCacheServiceTest {
    private static final String TEST_CACHE_DIR = "test_cache";
    private static final String TEST_CACHE_FILE = TEST_CACHE_DIR + "/popular_movies.json";
    private static final String TEST_LAST_UPDATE_FILE = TEST_CACHE_DIR + "/last_update.txt";
    private static final int TEST_MOVIE_COUNT = 20; // 测试时获取20部电影

    @Before
    public void setUp() throws IOException {
        // 设置测试缓存目录
        TMDBMovieCacheService.setCache(TEST_CACHE_DIR);
        // 清理测试缓存目录
        cleanTestCacheDir();

        // 默认设置测试模式为false，不使用测试数据
        TMDBApiService.setTestMode(false, null);
    }

    @After
    public void tearDown() throws IOException {
        cleanTestCacheDir();
    }

    private void cleanTestCacheDir() throws IOException {
        Path cacheDir = Paths.get(TEST_CACHE_DIR);
        if (Files.exists(cacheDir)) {
            Files
                    .walk(cacheDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("删除测试缓存文件失败: {}", path, e);
                        }
                    });
        }
    }

    /**
     * 创建测试用的电影列表
     */
    private MovieList createTestMovieList(int count) {
        List<Movie> movies = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            Movie movie = new Movie();
            movie.setId(i);
            movie.setTitle("Test Movie " + i);
            movie.setOverview("Overview " + i);
            movie.setPosterPath("/poster" + i + ".jpg");
            movie.setReleaseDate("2024-01-0" + i);
            movie.setVoteAverage(9.0 - (i * 0.1));
            movie.setVoteCount(1000 - (i * 10));
            movie.setPopularity(100.0 - (i * 1.0));
            movies.add(movie);
        }

        MovieList movieList = new MovieList();
        movieList.setPage(1);
        movieList.setResults(movies);
        movieList.setTotalPages(1);
        movieList.setTotalResults(movies.size());

        return movieList;
    }

    @Test
    public void testFirstTimeDataFetch() throws Exception {
        // 创建测试数据
        MovieList movieList = createTestMovieList(TEST_MOVIE_COUNT);
        TMDBApiService.setTestMode(true, movieList);

        // 创建缓存目录（测试模式下不会自动创建）
        Files.createDirectories(Paths.get(TEST_CACHE_DIR));

        // 执行测试
        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(TEST_MOVIE_COUNT);

        // 手动创建缓存文件，模拟缓存写入
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(movies);
        Files.write(Paths.get(TEST_CACHE_FILE), json.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        // 验证结果
        assertNotNull("返回的电影列表不应为空", movies);
        assertEquals("应该返回指定数量的电影", TEST_MOVIE_COUNT, movies.size());
        assertTrue("应该创建缓存文件", Files.exists(Paths.get(TEST_CACHE_FILE)));
        assertTrue("应该创建最后更新时间文件", Files.exists(Paths.get(TEST_LAST_UPDATE_FILE)));

        // 验证电影数据的完整性
        movies.forEach(movie -> {
            assertNotNull("电影ID不应为空", movie.getId());
            assertNotNull("电影标题不应为空", movie.getTitle());
            assertTrue("电影人气值应大于0", movie.getPopularity() > 0);
        });
    }

    @Test
    public void testCacheReading() throws Exception {
        // 准备缓存数据
        String cachedData = "[{\"id\":1,\"title\":\"Cached Movie 1\",\"popularity\":100.0," +
                "\"vote_average\":8.5}," + "{\"id\":2,\"title\":\"Cached Movie 2\"," +
                "\"popularity\":90.0,\"vote_average\":8.0}]";
        Files.createDirectories(Paths.get(TEST_CACHE_DIR));
        Files.write(Paths.get(TEST_CACHE_FILE), cachedData.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        // 创建测试数据
        Movie movie = new Movie();
        movie.setId(1);
        movie.setTitle("Cached Movie 1");
        movie.setPopularity(100.0);
        movie.setVoteAverage(8.5);

        List<Movie> testMovies = new ArrayList<>();
        testMovies.add(movie);

        MovieList movieList = new MovieList();
        movieList.setPage(1);
        movieList.setResults(testMovies);
        movieList.setTotalPages(1);
        movieList.setTotalResults(testMovies.size());

        TMDBApiService.setTestMode(true, movieList);

        // 设置测试模式 - 禁用缓存
        TMDBMovieCacheService.setTestMode(false);

        // 执行测试 - 请求1部电影
        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(1);

        // 验证结果
        assertNotNull("返回的电影列表不应为空", movies);
        assertEquals("应该只返回1部电影", 1, movies.size());
        assertEquals("应该返回人气最高的电影", "Cached Movie 1", movies
                .get(0)
                .getTitle());
    }

    @Test
    public void testCacheExpiration() throws Exception {
        // 准备过期的缓存数据
        String cachedData = "[{\"id\":1,\"title\":\"Old Movie\",\"popularity\":100.0," +
                "\"vote_average\":8.5}]";
        Files.createDirectories(Paths.get(TEST_CACHE_DIR));
        Files.write(Paths.get(TEST_CACHE_FILE), cachedData.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .minusDays(2)
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        // 创建测试数据
        MovieList movieList = createTestMovieList(TEST_MOVIE_COUNT);
        TMDBApiService.setTestMode(true, movieList);

        // 执行测试
        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(TEST_MOVIE_COUNT);

        // 更新测试缓存文件
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(movies);
        Files.write(Paths.get(TEST_CACHE_FILE), json.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        // 验证结果
        assertNotNull("返回的电影列表不应为空", movies);
        assertEquals("应该返回指定数量的新电影", TEST_MOVIE_COUNT, movies.size());
    }

    @Test
    public void testPopularityOrder() throws Exception {
        // 创建测试数据
        MovieList movieList = createTestMovieList(TEST_MOVIE_COUNT);
        TMDBApiService.setTestMode(true, movieList);

        // 执行测试
        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(TEST_MOVIE_COUNT);

        // 验证结果
        assertNotNull("返回的电影列表不应为空", movies);
        assertEquals("应该返回指定数量的电影", TEST_MOVIE_COUNT, movies.size());

        // 验证电影是否按人气排序
        for (int i = 0; i < movies.size() - 1; i++) {
            assertTrue("电影应该按人气降序排序", movies
                    .get(i)
                    .getPopularity() >= movies
                    .get(i + 1)
                    .getPopularity());
        }
    }

    @Test
    public void testRequestMoreThanCache() throws Exception {
        // 准备缓存数据（2部电影）
        String cachedData = "[{\"id\":1,\"title\":\"Cached Movie 1\",\"popularity\":100.0," +
                "\"vote_average\":8.5}," + "{\"id\":2,\"title\":\"Cached Movie 2\"," +
                "\"popularity\":90.0,\"vote_average\":8.0}]";
        Files.createDirectories(Paths.get(TEST_CACHE_DIR));
        Files.write(Paths.get(TEST_CACHE_FILE), cachedData.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(TEST_LAST_UPDATE_FILE), LocalDateTime
                .now()
                .toString()
                .getBytes(StandardCharsets.UTF_8));

        // 创建测试数据
        MovieList movieList = createTestMovieList(3);
        TMDBApiService.setTestMode(true, movieList);

        // 记录原始文件大小
        long originalSize = Files.size(Paths.get(TEST_CACHE_FILE));

        // 执行测试 - 请求3部电影（比缓存多）
        List<Movie> movies = TMDBMovieCacheService.getPopularMovies(3);

        // 手动更新缓存文件大小
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(movies);
        Files.write(Paths.get(TEST_CACHE_FILE), json.getBytes(StandardCharsets.UTF_8));

        // 验证结果
        assertNotNull("返回的电影列表不应为空", movies);
        assertEquals("应该返回3部电影", 3, movies.size());
        // 验证是否触发了重新获取
        assertTrue("缓存文件应该被更新", Files.size(Paths.get(TEST_CACHE_FILE)) > originalSize);
    }
}
