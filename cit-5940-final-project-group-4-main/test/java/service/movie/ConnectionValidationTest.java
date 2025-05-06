package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.game.Connection;
import model.game.GameSession;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBApiService;
import service.tmdbApi.TMDBMovieCacheService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 电影连接验证测试类
 * 专注于测试电影之间的连接关系验证
 */
@Slf4j
public class ConnectionValidationTest {

  private MovieIndexService indexService;
  private MovieDataService dataService;
  private List<Movie> testMovies;
  private MovieCredits credits1, credits2, credits3;
  private Movie movie1, movie2, movie3;
  private GameSession testSession;

  @Before
  public void setUp() {
    try {
      // 重置单例
      java.lang.reflect.Field instance = MovieIndexService.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);

      instance = MovieDataServiceImpl.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);

      // 设置测试模式
      MovieDataServiceImpl.setTestMode(true);
      TMDBMovieCacheService.setCache("test_cache");
      TMDBMovieCacheService.setTestMode(true);

      // 获取服务实例
      indexService = MovieIndexService.getInstance();
      dataService = MovieDataServiceImpl.getInstance();

      // 创建测试电影数据
      testMovies = createTestMovies();
      movie1 = testMovies.get(0);
      movie2 = testMovies.get(1);
      movie3 = testMovies.get(2);

      // 创建演职人员数据
      // movie1和movie2有共同演员"Actor 1"
      // movie2和movie3有共同导演"Director 2"
      credits1 = createMovieCredits(1, new int[] { 101, 102 }, new String[] { "Actor 1", "Actor 2" },
          new int[] { 201 }, new String[] { "Director 1" });

      credits2 = createMovieCredits(2, new int[] { 101, 103 }, new String[] { "Actor 1", "Actor 3" },
          new int[] { 202 }, new String[] { "Director 2" });

      credits3 = createMovieCredits(3, new int[] { 104, 105 }, new String[] { "Actor 4", "Actor 5" },
          new int[] { 202 }, new String[] { "Director 2" });

      // 初始化索引
      indexService.initializeIndexes(testMovies);
      indexService.indexMovieCredits(1, credits1);
      indexService.indexMovieCredits(2, credits2);
      indexService.indexMovieCredits(3, credits3);

      // 创建测试会话
      testSession = new GameSession("test-session", movie1);

    } catch (Exception e) {
      log.error("设置测试环境失败", e);
    }
  }

  @Test
  public void testValidateConnection_WithCommonActor() {
    // 测试有共同演员的电影连接验证
    boolean isValid = dataService.validateConnection(movie1, movie2);

    // 应该为true，因为movie1和movie2有共同演员"Actor 1"
    assertTrue(isValid);
  }

  @Test
  public void testValidateConnection_WithCommonDirector() {
    // 测试有共同导演的电影连接验证
    boolean isValid = dataService.validateConnection(movie2, movie3);

    // 应该为true，因为movie2和movie3有共同导演"Director 2"
    assertTrue(isValid);
  }

  @Test
  public void testValidateConnection_WithNoCommonPerson() {
    // 测试没有共同演职人员的电影连接验证
    boolean isValid = dataService.validateConnection(movie1, movie3);

    // 应该为false，因为movie1和movie3没有共同演职人员
    assertFalse(isValid);
  }

  @Test
  public void testGetConnections_WithCommonActor() {
    // 测试获取共同演员连接
    List<Connection> connections = dataService.getConnections(movie1, movie2);

    // 验证结果
    assertNotNull(connections);
    assertTrue(connections.size() > 0);

    // 验证连接类型和值
    boolean foundActorConnection = false;
    for (Connection connection : connections) {
      if ("演员".equals(connection.getConnectionType()) && "Actor 1".equals(connection.getConnectionValue())) {
        foundActorConnection = true;
        break;
      }
    }
    assertTrue("未找到共同演员连接", foundActorConnection);
  }

  @Test
  public void testGetConnections_WithCommonDirector() {
    // 测试获取共同导演连接
    List<Connection> connections = dataService.getConnections(movie2, movie3);

    // 验证结果
    assertNotNull(connections);
    assertTrue(connections.size() > 0);

    // 验证连接类型和值
    boolean foundDirectorConnection = false;
    for (Connection connection : connections) {
      if ("导演".equals(connection.getConnectionType()) && "Director 2".equals(connection.getConnectionValue())) {
        foundDirectorConnection = true;
        break;
      }
    }
    assertTrue("未找到共同导演连接", foundDirectorConnection);
  }

  @Test
  public void testGetConnections_WithNoCommonPerson() {
    // 测试获取没有共同演职人员的连接
    List<Connection> connections = dataService.getConnections(movie1, movie3);

    // 验证结果
    assertNotNull(connections);
    assertEquals(0, connections.size());
  }

  @Test
  public void testConnectionUsage() {
    // 获取movie1和movie2之间的连接
    List<Connection> connections = dataService.getConnections(movie1, movie2);
    assertNotNull(connections);
    assertTrue(connections.size() > 0);

    // 获取第一个连接
    Connection connection = connections.get(0);

    // 初始状态应该是未使用三次
    assertFalse(dataService.isConnectionUsedThreeTimes(connection, testSession));

    // 使用连接三次
    for (int i = 0; i < 3; i++) {
      dataService.registerUsedConnection(connection, testSession);
    }

    // 现在应该是已使用三次
    assertTrue(dataService.isConnectionUsedThreeTimes(connection, testSession));
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
    movie1.setGenreIds(new int[] { 28, 12 });

    Movie movie2 = new Movie();
    movie2.setId(2);
    movie2.setTitle("Movie 2");
    movie2.setOverview("Overview 2");
    movie2.setPosterPath("/poster2.jpg");
    movie2.setReleaseDate("2024-01-02");
    movie2.setVoteAverage(8.0);
    movie2.setVoteCount(900);
    movie2.setPopularity(90.0);
    movie2.setGenreIds(new int[] { 35, 18 });

    Movie movie3 = new Movie();
    movie3.setId(3);
    movie3.setTitle("Movie 3");
    movie3.setOverview("Overview 3");
    movie3.setPosterPath("/poster3.jpg");
    movie3.setReleaseDate("2024-01-03");
    movie3.setVoteAverage(7.5);
    movie3.setVoteCount(800);
    movie3.setPopularity(80.0);
    movie3.setGenreIds(new int[] { 80, 99 });

    movies.add(movie1);
    movies.add(movie2);
    movies.add(movie3);

    return movies;
  }

  /**
   * 创建电影演职人员数据
   * 
   * @param movieId       电影ID
   * @param actorIds      演员ID数组
   * @param actorNames    演员名称数组
   * @param directorIds   导演ID数组
   * @param directorNames 导演名称数组
   * @return 电影演职人员信息
   */
  private MovieCredits createMovieCredits(int movieId, int[] actorIds, String[] actorNames,
      int[] directorIds, String[] directorNames) {
    MovieCredits credits = new MovieCredits();
    credits.setId(movieId);

    // 创建演员列表
    List<CastMember> castList = new ArrayList<>();
    for (int i = 0; i < actorIds.length; i++) {
      CastMember cast = new CastMember();
      cast.setId(actorIds[i]);
      cast.setName(actorNames[i]);
      cast.setCharacter("Character " + i);
      cast.setOrder(i);
      castList.add(cast);
    }
    credits.setCast(castList);

    // 创建剧组成员列表
    List<CrewMember> crewList = new ArrayList<>();
    for (int i = 0; i < directorIds.length; i++) {
      CrewMember crew = new CrewMember();
      crew.setId(directorIds[i]);
      crew.setName(directorNames[i]);
      crew.setJob("Director");
      crew.setDepartment("Directing");
      crewList.add(crew);
    }
    credits.setCrew(crewList);

    return credits;
  }
}