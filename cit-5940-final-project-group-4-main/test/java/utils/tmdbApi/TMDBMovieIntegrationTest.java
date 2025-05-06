package utils.tmdbApi;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import org.junit.Test;
import service.tmdbApi.TMDBMovieService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;

/**
 * TMDB电影服务集成测试
 * 测试整个服务链路，包括API调用、缓存、多线程等功能
 */
@Slf4j
public class TMDBMovieIntegrationTest {
    private static final int DEMO_MOVIE_COUNT = 20; // 演示用，只获取20部电影

    @Test
    public void testCompleteFlow() {
        log.info("开始测试完整流程...");

        // 第一次获取（从API获取并缓存）
        Instant start = Instant.now();
        List<Movie> movies = TMDBMovieService.getTop5000PopularMovies();
        Duration duration = Duration.between(start, Instant.now());

        // 验证第一次获取结果
        assertNotNull("返回的电影列表不应为空", movies);
        assertFalse("应该返回电影数据", movies.isEmpty());
        log.info("第一次获取完成！耗时：{}秒，共获取到 {} 部电影", duration.getSeconds(), movies.size());

        // 打印部分电影信息
        printTopMovies(movies, DEMO_MOVIE_COUNT);

        // 第二次获取（从缓存读取）
        log.info("\n开始第二次获取（应该从缓存读取）...");
        start = Instant.now();
        List<Movie> cachedMovies = TMDBMovieService.getTop5000PopularMovies();
        duration = Duration.between(start, Instant.now());

        // 验证第二次获取结果
        assertNotNull("缓存的电影列表不应为空", cachedMovies);
        assertEquals("缓存应该返回相同数量的电影", movies.size(), cachedMovies.size());
        assertTrue("从缓存读取应该更快", duration.getSeconds() < 1);
        log.info("第二次获取完成！耗时：{}秒，共获取到 {} 部电影", duration.getSeconds(), cachedMovies.size());

        // 验证电影数据的完整性
        validateMovieData(movies);
    }

    @Test
    public void testPerformance() {
        log.info("开始性能测试...");

        // 预热缓存
        TMDBMovieService.getTop5000PopularMovies();

        // 测试10次缓存读取性能
        for (int i = 0; i < 10; i++) {
            Instant start = Instant.now();
            List<Movie> movies = TMDBMovieService.getTop5000PopularMovies();
            Duration duration = Duration.between(start, Instant.now());

            assertNotNull("返回的电影列表不应为空", movies);
            assertTrue("缓存读取应该很快", duration.toMillis() < 1000);
            log.info("第{}次读取耗时：{}毫秒", i + 1, duration.toMillis());
        }
    }

    /**
     * 验证电影数据的完整性
     */
    private void validateMovieData(List<Movie> movies) {
        movies.forEach(movie -> {
            assertNotNull("电影ID不应为空", movie.getId());
            assertNotNull("电影标题不应为空", movie.getTitle());
            assertTrue("电影人气值应大于0", movie.getPopularity() > 0);
            assertTrue("电影评分应在0-10之间", movie.getVoteAverage() >= 0 && movie.getVoteAverage() <= 10);
            assertTrue("评分数量应大于等于0", movie.getVoteCount() >= 0);
        });
    }

    /**
     * 打印电影信息
     */
    private void printTopMovies(List<Movie> movies, int count) {
        log.info("\n前{}部电影信息：", count);
        int limit = Math.min(count, movies.size());
        for (int i = 0; i < limit; i++) {
            Movie movie = movies.get(i);
            log.info("\n电影 #{}", i + 1);
            log.info("ID: {}", movie.getId());
            log.info("标题: {}", movie.getTitle());
            log.info("人气: {}", movie.getPopularity());
            log.info("评分: {} ({} 个评分)", movie.getVoteAverage(), movie.getVoteCount());
            log.info("上映日期: {}", movie.getReleaseDate());
            log.info("简介: {}", movie.getOverview());
        }
    }
}
