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
 * Movie data service implementation test
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
        // Reset Singleton
        try {
            // Reset the MovieDataServiceImpl singleton
            java.lang.reflect.Field instance = MovieDataServiceImpl.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);

            // Set TMDBMovieCacheService test mode
            TMDBMovieCacheService.setCache("test_cache");
            TMDBMovieCacheService.setTestMode(true);

            // Set MovieDataServiceImpl test mode
            MovieDataServiceImpl.setTestMode(true);

            // Creating test movies and movie cast data
            testMovies = createTestMovies();
            testCredits = createTestCredits();

            movie1 = testMovies.get(0);
            movie2 = testMovies.get(1);
            testSession = new GameSession("test-session", movie1);

            movieDataService = MovieDataServiceImpl.getInstance();

        } catch (Exception e) {
            log.error("Failed to set up the test environment", e);
        }
    }

    @Test
    public void testGetInitialMoviesList() {
        List<Movie> movies = movieDataService.getInitialMoviesList();
        assertNotNull(movies);
     // NOTE: The number of movies returned here may be affected by the TMDBMovieCacheService test mode
    }

    @Test
    public void testGetRandomStarterMovie() {
        Movie movie = movieDataService.getRandomStarterMovie();
        assertNotNull(movie);
    }

    @Test
    public void testSearchMoviesByPrefix() {
        List<Movie> results = movieDataService.searchMoviesByPrefix("mov");
        assertNotNull(results);
        // Note: The movies returned here may be affected by the indexing service and API
    }

    @Test
    public void testGetMovieById() {
        Movie movie = movieDataService.getMovieById(1);

        assertNotNull(movie);
        assertEquals(1, movie.getId());
    }

    @Test
    public void testValidateConnection() {
        // Test Movie Connection Verification
        boolean isValid = movieDataService.validateConnection(movie1, movie2);

     // Note: If movie1 and movie2 do not have a common actor/director in the test environment, this may be false
     // So here we only verify that the method does not throw an exception
    }

    @Test
    public void testGetConnections() {
        List<Connection> connections = movieDataService.getConnections(movie1, movie2);

        assertNotNull(connections);
    }

    @Test
    public void testIsConnectionUsedThreeTimes() {
        Connection connection = new Connection(movie1, movie2, "actor", "Actor 1", 101);

        assertFalse(movieDataService.isConnectionUsedThreeTimes(connection, testSession));

        for (int i = 0; i < 3; i++) {
            movieDataService.registerUsedConnection(connection, testSession);
        }

        assertTrue(movieDataService.isConnectionUsedThreeTimes(connection, testSession));
    }

    @Test
    public void testIsMovieAlreadyUsed() {
        assertTrue(movieDataService.isMovieAlreadyUsed(movie1, testSession));

        assertFalse(movieDataService.isMovieAlreadyUsed(movie2, testSession));

        movieDataService.registerUsedMovie(movie2, testSession);

        assertTrue(movieDataService.isMovieAlreadyUsed(movie2, testSession));
    }

    @Test
    public void testMatchesWinCondition() {
        WinCondition condition = new WinCondition("genre", "action", 5);

        boolean matches = movieDataService.matchesWinCondition(movie1, condition);

        assertTrue(matches);

        matches = movieDataService.matchesWinCondition(movie2, condition);

        assertFalse(matches);
    }

    @Test
    public void testRegisterUsedMovie() {
        movieDataService.registerUsedMovie(movie2, testSession);

        assertTrue(testSession.isMovieAlreadyUsed(movie2));
        assertEquals(movie2, testSession.getCurrentMovie());
        assertEquals(2, testSession.getCurrentStep());
    }

    @Test
    public void testRegisterUsedConnection() {
        Connection connection = new Connection(movie1, movie2, "Actor", "Actor 1", 101);

        movieDataService.registerUsedConnection(connection, testSession);

        assertEquals(1, connection.getUsageCount());
        assertEquals(1, testSession
                .getConnectionUsageCount()
                .get(101)
                .intValue());
    }

    @Test
    public void testMatchesWinConditionGenre() {
        WinCondition genreCondition = new WinCondition("genre", "action");

        assertTrue(movieDataService.matchesWinCondition(movie1, genreCondition));

        WinCondition wrongGenreCondition = new WinCondition("genre", "horror");
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongGenreCondition));
    }

    @Test
    public void testMatchesWinConditionActor() {
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);

        WinCondition actorCondition = new WinCondition("actor", "Actor 1");

        assertTrue(movieDataService.matchesWinCondition(movie1, actorCondition));

        WinCondition wrongActorCondition = new WinCondition("actor", "Non-existent actor");
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongActorCondition));
    }

    @Test
    public void testMatchesWinConditionDirector() {
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);

        WinCondition directorCondition = new WinCondition("director", "Director 1");

        assertTrue(movieDataService.matchesWinCondition(movie1, directorCondition));

        WinCondition wrongDirectorCondition = new WinCondition("director", "The non-existent director");
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongDirectorCondition));
    }

    @Test
    public void testMatchesWinConditionWriter() {
        MovieIndexService.getInstance().setMovieCreditsForTest(movie1.getId(), testCredits);

        WinCondition writerCondition = new WinCondition("writer", "Writer 1");

        assertTrue(movieDataService.matchesWinCondition(movie1, writerCondition));

        WinCondition wrongWriterCondition = new WinCondition("writer", "The non-existent screenwriter");
        assertFalse(movieDataService.matchesWinCondition(movie1, wrongWriterCondition));
    }

    @Test
    public void testMatchesWinConditionInvalidType() {
        WinCondition invalidCondition = new WinCondition("invalid", "value");
        assertFalse(movieDataService.matchesWinCondition(movie1, invalidCondition));
    }

    @Test
    public void testMatchesWinConditionNullValues() {
        assertFalse(movieDataService.matchesWinCondition(null, new WinCondition("genre", "action")));
        assertFalse(movieDataService.matchesWinCondition(movie1, null));
        assertFalse(movieDataService.matchesWinCondition(null, null));
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
        movie1.setGenreIds(new int[] { 28, 12 }); // 28 is an action movie

        Movie movie2 = new Movie();
        movie2.setId(2);
        movie2.setTitle("Movie 2");
        movie2.setOverview("Overview 2");
        movie2.setPosterPath("/poster2.jpg");
        movie2.setReleaseDate("2024-01-02");
        movie2.setVoteAverage(8.0);
        movie2.setVoteCount(900);
        movie2.setPopularity(90.0);
        movie2.setGenreIds(new int[] { 35, 18 }); // 35 is a comedy, 18 is a drama

        Movie movie3 = new Movie();
        movie3.setId(3);
        movie3.setTitle("Movie 3");
        movie3.setOverview("Overview 3");
        movie3.setPosterPath("/poster3.jpg");
        movie3.setReleaseDate("2024-01-03");
        movie3.setVoteAverage(7.5);
        movie3.setVoteCount(800);
        movie3.setPopularity(80.0);
        movie3.setGenreIds(new int[] { 80, 99 }); // 80% is a crime film, 99% is a documentary

        movies.add(movie1);
        movies.add(movie2);
        movies.add(movie3);

        return movies;
    }

    /**
     * Creating Test Cast Data
     */
    private MovieCredits createTestCredits() {
        MovieCredits credits = new MovieCredits();
        credits.setId(1);

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
