package service.movie;

import lombok.extern.slf4j.Slf4j;
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
 * 胜利条件测试类
 * 专注于测试游戏胜利条件的判断
 */
@Slf4j
public class WinConditionTest {

  private MovieDataService dataService;
  private MovieIndexService indexService;
  private List<Movie> testMovies;
  private MovieCredits credit1, credit2, credit3;
  private Movie actionMovie, comedyMovie, crimeMovie;

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
      actionMovie = testMovies.get(0); // 动作片
      comedyMovie = testMovies.get(1); // 喜剧片
      crimeMovie = testMovies.get(2); // 犯罪片

      // 创建演职人员数据
      credit1 = createMovieCreditsWithActor(1, 101, "Actor 1");
      credit2 = createMovieCreditsWithActor(2, 102, "Actor 2");
      credit3 = createMovieCreditsWithActor(3, 103, "Actor 3");

      // 创建导演信息
      addDirectorToCredits(credit1, 201, "Director 1");
      addDirectorToCredits(credit2, 202, "Director 2");
      addDirectorToCredits(credit3, 203, "Director 3");

      // 初始化索引
      indexService.initializeIndexes(testMovies);
      indexService.indexMovieCredits(1, credit1);
      indexService.indexMovieCredits(2, credit2);
      indexService.indexMovieCredits(3, credit3);

    } catch (Exception e) {
      log.error("设置测试环境失败", e);
    }
  }

  @Test
  public void testMatchesWinCondition_Genre() {
    // 创建类型胜利条件：动作片
    WinCondition actionCondition = new WinCondition("genre", "动作", 1);

    // 验证动作片匹配
    assertTrue("动作片应该匹配动作胜利条件",
        dataService.matchesWinCondition(actionMovie, actionCondition));

    // 验证喜剧片不匹配
    assertFalse("喜剧片不应该匹配动作胜利条件",
        dataService.matchesWinCondition(comedyMovie, actionCondition));

    // 创建类型胜利条件：喜剧片
    WinCondition comedyCondition = new WinCondition("genre", "喜剧", 1);

    // 验证喜剧片匹配
    assertTrue("喜剧片应该匹配喜剧胜利条件",
        dataService.matchesWinCondition(comedyMovie, comedyCondition));
  }

  @Test
  public void testMatchesWinCondition_Actor() {
    // 创建演员胜利条件：Actor 1
    WinCondition actorCondition = new WinCondition("actor", "Actor 1", 1);

    // 验证包含Actor 1的电影匹配
    assertTrue("包含Actor 1的电影应该匹配Actor 1胜利条件",
        dataService.matchesWinCondition(actionMovie, actorCondition));

    // 验证不包含Actor 1的电影不匹配
    assertFalse("不包含Actor 1的电影不应该匹配Actor 1胜利条件",
        dataService.matchesWinCondition(comedyMovie, actorCondition));
  }

  @Test
  public void testMatchesWinCondition_Director() {
    // 创建导演胜利条件：Director 1
    WinCondition directorCondition = new WinCondition("director", "Director 1", 1);

    // 验证包含Director 1的电影匹配
    assertTrue("包含Director 1的电影应该匹配Director 1胜利条件",
        dataService.matchesWinCondition(actionMovie, directorCondition));

    // 验证不包含Director 1的电影不匹配
    assertFalse("不包含Director 1的电影不应该匹配Director 1胜利条件",
        dataService.matchesWinCondition(comedyMovie, directorCondition));
  }

  @Test
  public void testWinConditionProgress() {
    // 创建胜利条件：需要3部动作片
    WinCondition condition = new WinCondition("genre", "动作", 3);

    // 初始进度应该为0
    assertEquals(0, condition.getCurrentCount());
    assertFalse(condition.isAchieved());

    // 增加1次进度
    condition.incrementProgress();
    assertEquals(1, condition.getCurrentCount());
    assertFalse(condition.isAchieved());

    // 增加到3次进度
    condition.incrementProgress();
    condition.incrementProgress();
    assertEquals(3, condition.getCurrentCount());
    assertTrue(condition.isAchieved());
  }

  /**
   * 创建测试电影数据
   */
  private List<Movie> createTestMovies() {
    List<Movie> movies = new ArrayList<>();

    // 创建一部动作片
    Movie actionMovie = new Movie();
    actionMovie.setId(1);
    actionMovie.setTitle("Action Movie");
    actionMovie.setOverview("An action movie");
    actionMovie.setPosterPath("/poster1.jpg");
    actionMovie.setReleaseDate("2024-01-01");
    actionMovie.setVoteAverage(8.5);
    actionMovie.setVoteCount(1000);
    actionMovie.setPopularity(100.0);
    actionMovie.setGenreIds(new int[] { 28, 12 }); // 28是动作片, 12是冒险片

    // 创建一部喜剧片
    Movie comedyMovie = new Movie();
    comedyMovie.setId(2);
    comedyMovie.setTitle("Comedy Movie");
    comedyMovie.setOverview("A comedy movie");
    comedyMovie.setPosterPath("/poster2.jpg");
    comedyMovie.setReleaseDate("2024-01-02");
    comedyMovie.setVoteAverage(8.0);
    comedyMovie.setVoteCount(900);
    comedyMovie.setPopularity(90.0);
    comedyMovie.setGenreIds(new int[] { 35, 10749 }); // 35是喜剧片, 10749是爱情片

    // 创建一部犯罪片
    Movie crimeMovie = new Movie();
    crimeMovie.setId(3);
    crimeMovie.setTitle("Crime Movie");
    crimeMovie.setOverview("A crime movie");
    crimeMovie.setPosterPath("/poster3.jpg");
    crimeMovie.setReleaseDate("2024-01-03");
    crimeMovie.setVoteAverage(7.5);
    crimeMovie.setVoteCount(800);
    crimeMovie.setPopularity(80.0);
    crimeMovie.setGenreIds(new int[] { 80, 53 }); // 80是犯罪片, 53是惊悚片

    movies.add(actionMovie);
    movies.add(comedyMovie);
    movies.add(crimeMovie);

    return movies;
  }

  /**
   * 创建电影演员数据
   */
  private MovieCredits createMovieCreditsWithActor(int movieId, int actorId, String actorName) {
    MovieCredits credits = new MovieCredits();
    credits.setId(movieId);

    // 创建演员列表
    List<CastMember> castList = new ArrayList<>();

    CastMember cast = new CastMember();
    cast.setId(actorId);
    cast.setName(actorName);
    cast.setCharacter("Character");
    cast.setOrder(1);

    castList.add(cast);
    credits.setCast(castList);

    // 初始化空的剧组成员列表
    credits.setCrew(new ArrayList<>());

    return credits;
  }

  /**
   * 添加导演到演职人员信息
   */
  private void addDirectorToCredits(MovieCredits credits, int directorId, String directorName) {
    CrewMember director = new CrewMember();
    director.setId(directorId);
    director.setName(directorName);
    director.setJob("Director");
    director.setDepartment("Directing");

    credits.getCrew().add(director);
  }
}