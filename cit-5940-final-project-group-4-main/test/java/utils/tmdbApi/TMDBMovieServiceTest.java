package utils.tmdbApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import model.tmdb.MovieList;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBApiService;
import service.tmdbApi.TMDBMovieCacheService;
import service.tmdbApi.TMDBMovieService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TMDB电影服务测试类
 */
@Slf4j
public class TMDBMovieServiceTest {
    private MockWebServer mockWebServer;
    private String originalBaseUrl;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        originalBaseUrl = TMDBMovieService.getBaseUrl();
        TMDBMovieService.setBaseUrl("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort());

        // 设置测试缓存目录
        TMDBMovieCacheService.setCache("test_cache");
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        TMDBMovieService.setBaseUrl(originalBaseUrl);
    }

    @Test
    public void testGetPopularMovies() throws Exception {
        // 创建测试用的电影列表
        List<Movie> moviesList = createTestMovies();

        // 创建MovieList对象
        MovieList movieList = new MovieList();
        movieList.setPage(1);
        movieList.setResults(moviesList);
        movieList.setTotalPages(1);
        movieList.setTotalResults(moviesList.size());

        // 设置API服务使用测试模式
        TMDBApiService.setTestMode(true, movieList);

        // 执行请求 - 使用getPopularMovies方法
        List<Movie> movies = TMDBMovieService.getPopularMovies(3);

        // 打印实际情况进行调试
        if (movies != null && !movies.isEmpty()) {
            log.error("实际返回电影数量: {}", movies.size());
            for (Movie movie : movies) {
                log.error("电影ID: {}, 标题: {}, 人气值: {}", movie.getId(), movie.getTitle(),
                          movie.getPopularity());
            }
        }

        // 验证结果
        assertNotNull(movies);
        assertEquals(3, movies.size());

        // 验证第一部电影
        Movie firstMovie = movies.get(0);
        assertEquals(1, firstMovie.getId());
        assertEquals("Movie 1", firstMovie.getTitle());
        assertEquals(8.5, firstMovie.getVoteAverage(), 0.001);
        assertEquals(100.0, firstMovie.getPopularity(), 0.001);

        // 验证第三部电影
        Movie lastMovie = movies.get(2);
        assertEquals(3, lastMovie.getId());
        assertEquals("Movie 3", lastMovie.getTitle());
        assertEquals(7.5, lastMovie.getVoteAverage(), 0.001);
        assertEquals(80.0, lastMovie.getPopularity(), 0.001);
    }

    /**
     * 创建测试用的电影列表
     */
    private List<Movie> createTestMovies() {
        List<Movie> movies = new ArrayList<>();

        // 创建三部测试电影
        Movie movie1 = new Movie();
        movie1.setId(1);
        movie1.setTitle("Movie 1");
        movie1.setOverview("Overview 1");
        movie1.setPosterPath("/poster1.jpg");
        movie1.setReleaseDate("2024-01-01");
        movie1.setVoteAverage(8.5);
        movie1.setVoteCount(1000);
        movie1.setPopularity(100.0);

        Movie movie2 = new Movie();
        movie2.setId(2);
        movie2.setTitle("Movie 2");
        movie2.setOverview("Overview 2");
        movie2.setPosterPath("/poster2.jpg");
        movie2.setReleaseDate("2024-01-02");
        movie2.setVoteAverage(8.0);
        movie2.setVoteCount(900);
        movie2.setPopularity(90.0);

        Movie movie3 = new Movie();
        movie3.setId(3);
        movie3.setTitle("Movie 3");
        movie3.setOverview("Overview 3");
        movie3.setPosterPath("/poster3.jpg");
        movie3.setReleaseDate("2024-01-03");
        movie3.setVoteAverage(7.5);
        movie3.setVoteCount(800);
        movie3.setPopularity(80.0);

        movies.add(movie1);
        movies.add(movie2);
        movies.add(movie3);

        return movies;
    }
}
