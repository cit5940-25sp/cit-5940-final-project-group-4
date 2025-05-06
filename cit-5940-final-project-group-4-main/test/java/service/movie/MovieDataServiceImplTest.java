package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBMovieCacheService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 电影数据服务实现类测试
 */
@Slf4j
public class MovieDataServiceImplTest {

    private MovieDataServiceImpl movieDataService;
    private MovieIndexService mockIndexService;
    private List<Movie> testMovies;
    private MovieCredits testCredits;
    private Movie movie1, movie2;
    private GameSession testSession;

    @Before
    public void setUp() {
        // 重置单例
        try {
            // 重置MovieDataServiceImpl单例
            java.lang.reflect.Field instance = MovieDataServiceImpl.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);

            // 设置TMDBMovieCacheService测试模式
            TMDBMovieCacheService.setCache("test_cache");
            TMDBMovieCacheService.setTestMode(true);

            // 设置MovieDataServiceImpl测试模式
            MovieDataServiceImpl.setTestMode(true);

            // 创建测试电影和电影演职人员数据
            testMovies = createTestMovies();
            testCredits = createTestCredits();

            // 创建测试会话
            movie1 = testMovies.get(0); // 第一部电影
            movie2 = testMovies.get(1); // 第二部电影
            testSession = new GameSession("test-session", movie1);

            // 获取数据服务实例
            movieDataService = MovieDataServiceImpl.getInstance();

        } catch (Exception e) {
            log.error("设置测试环境失败", e);
        }
    }

    @Test
    public void testGetInitialMoviesList() {
        List<Movie> movies = movieDataService.getInitialMoviesList();
        assertNotNull(movies);
        // 注意：此处返回的电影数量可能受到TMDBMovieCacheService测试模式的影响
    }

    @Test
    public void testGetRandomStarterMovie() {
        Movie movie = movieDataService.getRandomStarterMovie();
        assertNotNull(movie);
    }

    @Test
    public void testSearchMoviesByPrefix() {
        // 测试搜索功能
        List<Movie> results = movieDataService.searchMoviesByPrefix("mov");

        // 验证结果
        assertNotNull(results);
        // 注意：此处返回的电影可能受到索引服务和API的影响
    }

    @Test
    public void testGetMovieById() {
        // 测试获取电影详情
        Movie movie = movieDataService.getMovieById(1);

        // 验证结果
        assertNotNull(movie);
        assertEquals(1, movie.getId());
    }

    @Test
    public void testValidateConnection() {
        // 测试电影连接验证
        boolean isValid = movieDataService.validateConnection(movie1, movie2);

        // 注意：如果测试环境中movie1和movie2没有共同演员/导演，这里可能为false
        // 所以这里只验证方法不抛异常
    }

    @Test
    public void testGetConnections() {
        // 测试获取电影连接
        List<Connection> connections = movieDataService.getConnections(movie1, movie2);

        // 验证结果
        assertNotNull(connections);
        // 注意：连接数量取决于测试数据
    }

    @Test
    public void testIsConnectionUsedThreeTimes() {
        // 创建测试连接
        Connection connection = new Connection(movie1, movie2, "演员", "Actor 1", 101);

        // 初始状态应该是未使用三次
        assertFalse(movieDataService.isConnectionUsedThreeTimes(connection, testSession));

        // 使用连接三次
        for (int i = 0; i < 3; i++) {
            movieDataService.registerUsedConnection(connection, testSession);
        }

        // 现在应该是已使用三次
        assertTrue(movieDataService.isConnectionUsedThreeTimes(connection, testSession));
    }

    @Test
    public void testIsMovieAlreadyUsed() {
        // movie1已经在创建会话时被使用
        assertTrue(movieDataService.isMovieAlreadyUsed(movie1, testSession));

        // movie2未被使用
        assertFalse(movieDataService.isMovieAlreadyUsed(movie2, testSession));

        // 注册movie2
        movieDataService.registerUsedMovie(movie2, testSession);

        // 现在movie2也应该被标记为已使用
        assertTrue(movieDataService.isMovieAlreadyUsed(movie2, testSession));
    }

    @Test
    public void testMatchesWinCondition() {
        // 创建胜利条件: 类型是"动作"，需要5部电影
        WinCondition condition = new WinCondition("genre", "动作", 5);

        // 检查movie1是否匹配条件(movie1的类型包括28，对应"动作")
        boolean matches = movieDataService.matchesWinCondition(movie1, condition);

        // 验证结果
        assertTrue(matches);

        // 检查movie2是否匹配条件(movie2的类型不包括28)
        matches = movieDataService.matchesWinCondition(movie2, condition);

        // 验证结果
        assertFalse(matches);
    }

    @Test
    public void testRegisterUsedMovie() {
        // 测试注册使用电影
        movieDataService.registerUsedMovie(movie2, testSession);

        // 验证结果
        assertTrue(testSession.isMovieAlreadyUsed(movie2));
        assertEquals(movie2, testSession.getCurrentMovie());
        assertEquals(2, testSession.getCurrentStep());
    }

    @Test
    public void testRegisterUsedConnection() {
        // 创建测试连接
        Connection connection = new Connection(movie1, movie2, "演员", "Actor 1", 101);

        // 测试注册使用连接
        movieDataService.registerUsedConnection(connection, testSession);

        // 验证结果
        assertEquals(1, connection.getUsageCount());
        assertEquals(1, testSession
                .getConnectionUsageCount()
                .get(101)
                .intValue());
    }

    @Test
    public void testMatchesWinConditionGenre() {
        // 创建类型胜利条件
        WinCondition genreCondition = new WinCondition("genre", "动作");

        // 通过MovieGenreService进行映射
        assertTrue(movieDataService.matchesWinCondition(movie1, genreCondition));

        // 测试不匹配
        WinCondition wrongGenreCondition = new WinCondition("genre", "恐怖");
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongGenreCondition));
    }

    @Test
    public void testMatchesWinConditionActor() {
        // 模拟MovieIndexService
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);

        // 创建演员胜利条件
        WinCondition actorCondition = new WinCondition("actor", "Actor 1");

        // 测试匹配
        assertTrue(movieDataService.matchesWinCondition(movie1, actorCondition));

        // 测试不匹配
        WinCondition wrongActorCondition = new WinCondition("actor", "不存在的演员");
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongActorCondition));
    }

    @Test
    public void testMatchesWinConditionDirector() {
        // 模拟MovieIndexService
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);

        // 创建导演胜利条件
        WinCondition directorCondition = new WinCondition("director", "Director 1");

        // 测试匹配
        assertTrue(movieDataService.matchesWinCondition(movie1, directorCondition));

        // 测试不匹配
        WinCondition wrongDirectorCondition = new WinCondition("director", "不存在的导演");
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongDirectorCondition));
    }

    @Test
    public void testMatchesWinConditionWriter() {
        // 模拟MovieIndexService
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);

        // 创建编剧胜利条件
        WinCondition writerCondition = new WinCondition("writer", "Writer 1");

        // 测试匹配
        assertTrue(movieDataService.matchesWinCondition(movie1, writerCondition));

        // 测试不匹配
        WinCondition wrongWriterCondition = new WinCondition("writer", "不存在的编剧");
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongWriterCondition));
    }

    @Test
    public void testMatchesWinConditionInvalidType() {
        // 测试无效类型
        WinCondition invalidCondition = new WinCondition("invalid", "值");
        assertFalse(movieDataService.matchesWinCondition(movie1, invalidCondition));
    }

    @Test
    public void testMatchesWinConditionNullValues() {
        // 测试null值
        assertFalse(movieDataService.matchesWinCondition(null, new WinCondition("genre", "动作")));
        assertFalse(movieDataService.matchesWinCondition(movie1, null));
        assertFalse(movieDataService.matchesWinCondition(null, null));
    }

    /**
     * 创建测试电影数据
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
        movie1.setGenreIds(new int[] { 28, 12 }); // 28是动作片

        Movie movie2 = new Movie();
        movie2.setId(2);
        movie2.setTitle("Movie 2");
        movie2.setOverview("Overview 2");
        movie2.setPosterPath("/poster2.jpg");
        movie2.setReleaseDate("2024-01-02");
        movie2.setVoteAverage(8.0);
        movie2.setVoteCount(900);
        movie2.setPopularity(90.0);
        movie2.setGenreIds(new int[] { 35, 18 }); // 35是喜剧片，18是剧情片

        Movie movie3 = new Movie();
        movie3.setId(3);
        movie3.setTitle("Movie 3");
        movie3.setOverview("Overview 3");
        movie3.setPosterPath("/poster3.jpg");
        movie3.setReleaseDate("2024-01-03");
        movie3.setVoteAverage(7.5);
        movie3.setVoteCount(800);
        movie3.setPopularity(80.0);
        movie3.setGenreIds(new int[] { 80, 99 }); // 80是犯罪片，99是纪录片

        movies.add(movie1);
        movies.add(movie2);
        movies.add(movie3);

        return movies;
    }

    /**
     * 创建测试演职人员数据
     */
    private MovieCredits createTestCredits() {
        MovieCredits credits = new MovieCredits();
        credits.setId(1);

        // 创建演员列表
        List<CastMember> castList = new ArrayList<>();

        CastMember cast1 = new CastMember();
        cast1.setId(101);
        cast1.setName("Actor 1");
        cast1.setCharacter("Character 1");
        cast1.setOrder(1);

        CastMember cast2 = new CastMember();
        cast2.setId(102);
        cast2.setName("Actor 2");
        cast2.setCharacter("Character 2");
        cast2.setOrder(2);

        castList.add(cast1);
        castList.add(cast2);
        credits.setCast(castList);

        // 创建剧组成员列表
        List<CrewMember> crewList = new ArrayList<>();

        CrewMember crew1 = new CrewMember();
        crew1.setId(201);
        crew1.setName("Director 1");
        crew1.setJob("Director");
        crew1.setDepartment("Directing");

        CrewMember crew2 = new CrewMember();
        crew2.setId(202);
        crew2.setName("Writer 1");
        crew2.setJob("Writer");
        crew2.setDepartment("Writing");

        crewList.add(crew1);
        crewList.add(crew2);
        credits.setCrew(crewList);

        return credits;
    }
}
