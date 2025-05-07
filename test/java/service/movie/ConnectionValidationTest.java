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
* Movie connection verification test class
* Focus on testing the connection relationship verification between movies
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
      java.lang.reflect.Field instance = MovieIndexService.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);

      instance = MovieDataServiceImpl.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);

      MovieDataServiceImpl.setTestMode(true);
      TMDBMovieCacheService.setCache("test_cache");
      TMDBMovieCacheService.setTestMode(true);

      indexService = MovieIndexService.getInstance();
      dataService = MovieDataServiceImpl.getInstance();

      testMovies = createTestMovies();
      movie1 = testMovies.get(0);
      movie2 = testMovies.get(1);
      movie3 = testMovies.get(2);

       // Create cast and crew data
       // movie1 and movie2 have a common actor "Actor 1"
       // movie2 and movie3 have a common director "Director 2"
      credits1 = createMovieCredits(1, new int[] { 101, 102 }, new String[] { "Actor 1", "Actor 2" },
          new int[] { 201 }, new String[] { "Director 1" });

      credits2 = createMovieCredits(2, new int[] { 101, 103 }, new String[] { "Actor 1", "Actor 3" },
          new int[] { 202 }, new String[] { "Director 2" });

      credits3 = createMovieCredits(3, new int[] { 104, 105 }, new String[] { "Actor 4", "Actor 5" },
          new int[] { 202 }, new String[] { "Director 2" });

      indexService.initializeIndexes(testMovies);
      indexService.indexMovieCredits(1, credits1);
      indexService.indexMovieCredits(2, credits2);
      indexService.indexMovieCredits(3, credits3);

      testSession = new GameSession("test-session", movie1);

    } catch (Exception e) {
      log.error("Failed to set up the test environment", e);
    }
  }

  @Test
  public void testValidateConnection_WithCommonActor() {
    boolean isValid = dataService.validateConnection(movie1, movie2);

    // Should be true, because movie1 and movie2 have a common actor "Actor 1"
    assertTrue(isValid);
  }

  @Test
  public void testValidateConnection_WithCommonDirector() {
    boolean isValid = dataService.validateConnection(movie2, movie3);

    // Should be true, because movie2 and movie3 have the same director "Director 2"
    assertTrue(isValid);
  }

  @Test
  public void testValidateConnection_WithNoCommonPerson() {
    boolean isValid = dataService.validateConnection(movie1, movie3);

    // Should be false, because movie1 and movie3 have no co-stars
    assertFalse(isValid);
  }

  @Test
  public void testGetConnections_WithCommonActor() {
    List<Connection> connections = dataService.getConnections(movie1, movie2);

    assertNotNull(connections);
    assertTrue(connections.size() > 0);

    boolean foundActorConnection = false;
    for (Connection connection : connections) {
      if ("actor".equals(connection.getConnectionType()) && "Actor 1".equals(connection.getConnectionValue())) {
        foundActorConnection = true;
        break;
      }
    }
    assertTrue("No co-actor connections found", foundActorConnection);
  }

  @Test
  public void testGetConnections_WithCommonDirector() {
    List<Connection> connections = dataService.getConnections(movie2, movie3);

    assertNotNull(connections);
    assertTrue(connections.size() > 0);

    boolean foundDirectorConnection = false;
    for (Connection connection : connections) {
      if ("director".equals(connection.getConnectionType()) && "Director 2".equals(connection.getConnectionValue())) {
        foundDirectorConnection = true;
        break;
      }
    }
    assertTrue("No co-director connection found", foundDirectorConnection);
  }

  @Test
  public void testGetConnections_WithNoCommonPerson() {
    List<Connection> connections = dataService.getConnections(movie1, movie3);
    assertNotNull(connections);
    assertEquals(0, connections.size());
  }

  @Test
  public void testConnectionUsage() {
    List<Connection> connections = dataService.getConnections(movie1, movie2);
    assertNotNull(connections);
    assertTrue(connections.size() > 0);

    Connection connection = connections.get(0);

    assertFalse(dataService.isConnectionUsedThreeTimes(connection, testSession));

    for (int i = 0; i < 3; i++) {
      dataService.registerUsedConnection(connection, testSession);
    }

    assertTrue(dataService.isConnectionUsedThreeTimes(connection, testSession));
  }

  /**
   * Creating test movie data
   */
  private List<Movie> createTestMovies() {
    List<Movie> movies = new ArrayList<>();

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
  * Create movie cast data
  *
  * @param movieId movie ID
  * @param actorIds actor ID array
  * @param actorNames actor name array
  * @param directorIds director ID array
  * @param directorNames director name array
  * @return movie cast information
  */
  private MovieCredits createMovieCredits(int movieId, int[] actorIds, String[] actorNames,
      int[] directorIds, String[] directorNames) {
    MovieCredits credits = new MovieCredits();
    credits.setId(movieId);

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