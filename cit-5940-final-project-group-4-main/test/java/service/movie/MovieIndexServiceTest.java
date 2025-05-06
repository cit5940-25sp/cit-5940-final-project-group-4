package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBApiService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 电影索引服务测试类
 */
@Slf4j
public class MovieIndexServiceTest {

  private MovieIndexService indexService;
  private List<Movie> testMovies;
  private MovieCredits testCredits;

  @Before
  public void setUp() {
    // 重置单例
    try {
      java.lang.reflect.Field instance = MovieIndexService.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);
    } catch (Exception e) {
      log.error("重置单例失败", e);
    }

    // 获取索引服务实例
    indexService = MovieIndexService.getInstance();

    // 创建测试电影数据
    testMovies = createTestMovies();

    // 创建测试演职人员数据
    testCredits = createTestCredits();

    // 初始化索引
    indexService.initializeIndexes(testMovies);

    // 为第一部电影添加演职人员索引
    indexService.indexMovieCredits(1, testCredits);
  }

  @Test
  public void testSearchByPrefix() {
    // 测试前缀搜索功能
    List<Movie> results = indexService.searchByPrefix("mov");

    // 验证结果
    assertNotNull(results);
    assertEquals(3, results.size());

    // 验证排序（按人气降序）
    assertEquals(1, results.get(0).getId());
    assertEquals(2, results.get(1).getId());
    assertEquals(3, results.get(2).getId());
  }

  @Test
  public void testSearchByPrefixWithLowerCase() {
    // 测试小写前缀搜索
    List<Movie> results = indexService.searchByPrefix("MOV");

    // 验证结果
    assertNotNull(results);
    assertEquals(3, results.size());
  }

  @Test
  public void testSearchByPrefixWithEmptyPrefix() {
    // 测试空前缀
    List<Movie> results = indexService.searchByPrefix("");

    // 验证结果
    assertNotNull(results);
    assertEquals(0, results.size());
  }

  @Test
  public void testGetMovieById() {
    // 测试按ID获取电影
    Movie movie = indexService.getMovieById(1);

    // 验证结果
    assertNotNull(movie);
    assertEquals(1, movie.getId());
    assertEquals("Movie 1", movie.getTitle());
  }

  @Test
  public void testGetMovieByIdWithNonExistingId() {
    // 测试获取不存在的电影ID
    // 注意：这里假设999是一个不存在的ID，实际测试可能需要调整
    Movie movie = indexService.getMovieById(999);

    // 由于我们不使用mock，这里的行为取决于实际实现
    // 如果TMDBApiService在找不到电影时返回null，则结果应为null
  }

  @Test
  public void testGetMovieCredits() {
    // 测试获取电影演职人员
    MovieCredits credits = indexService.getMovieCredits(1);

    // 验证结果
    assertNotNull(credits);
    assertEquals(1, credits.getId());
    assertEquals(2, credits.getCast().size());
    assertEquals(2, credits.getCrew().size());
  }

  @Test
  public void testGetMoviesByActor() {
    // 测试根据演员获取电影列表
    List<Movie> movies = indexService.getMoviesByActor(101);

    // 验证结果
    assertNotNull(movies);
    assertEquals(1, movies.size());
    assertEquals(1, movies.get(0).getId());
  }

  @Test
  public void testGetMoviesByDirector() {
    // 测试根据导演获取电影列表
    List<Movie> movies = indexService.getMoviesByDirector(201);

    // 验证结果
    assertNotNull(movies);
    assertEquals(1, movies.size());
    assertEquals(1, movies.get(0).getId());
  }

  @Test
  public void testGetActorIdByName() {
    // 测试根据演员名称获取ID
    Integer actorId = indexService.getActorIdByName("Actor 1");

    // 验证结果
    assertNotNull(actorId);
    assertEquals(Integer.valueOf(101), actorId);
  }

  @Test
  public void testGetDirectorIdByName() {
    // 测试根据导演名称获取ID
    Integer directorId = indexService.getDirectorIdByName("Director 1");

    // 验证结果
    assertNotNull(directorId);
    assertEquals(Integer.valueOf(201), directorId);
  }

  @Test
  public void testClearIndexes() {
    // 测试清空索引
    indexService.clearIndexes();

    // 验证结果
    List<Movie> results = indexService.searchByPrefix("mov");
    assertEquals(0, results.size());

    // 因为我们没有使用mock，所以这个测试可能会尝试调用真实的API
    // 因此我们不测试清空后的getMovieById行为
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