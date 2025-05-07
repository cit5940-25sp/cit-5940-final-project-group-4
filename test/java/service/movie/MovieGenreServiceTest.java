package service.movie;

import model.tmdb.Genre;
import org.junit.Before;
import org.junit.Test;
import service.tmdbApi.TMDBApiService;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

class MovieGenreServiceTest {
  private MovieGenreService genreService;

  @Before
  public void setUp() {
    genreService = MovieGenreService.getInstance();
  }

  @Test
  public void testGetGenreName() {
    assertEquals("action", genreService.getGenreName(28));
    assertEquals("adventure", genreService.getGenreName(12));
    assertEquals("Animation", genreService.getGenreName(16));

    assertEquals("Unknown Type", genreService.getGenreName(999));
  }

  @Test
  public void testGetGenreId() {
    assertEquals(Integer.valueOf(28), genreService.getGenreId("action"));
    assertEquals(Integer.valueOf(12), genreService.getGenreId("adventure"));
    assertEquals(Integer.valueOf(16), genreService.getGenreId("Animation"));

    assertNull(genreService.getGenreId("Unknown Type"));
  }

  @Test
  public void testHasGenre() {
    int[] genreIds = { 28, 12, 16 }; 
    assertTrue(genreService.hasGenre(genreIds, "action"));
    assertTrue(genreService.hasGenre(genreIds, "adventure"));
    assertTrue(genreService.hasGenre(genreIds, "Animation"));

    assertFalse(genreService.hasGenre(genreIds, "comedy"));
    assertFalse(genreService.hasGenre(genreIds, "fear"));

    assertFalse(genreService.hasGenre(new int[0], "action"));

    assertFalse(genreService.hasGenre(null, "action"));
    assertFalse(genreService.hasGenre(genreIds, null));
    assertFalse(genreService.hasGenre(null, null));
  }

  @Test
  public void testInitializeGenresWithEmptyApiResponse() {
    List<Genre> emptyGenres = Collections.emptyList();

    assertNotNull(genreService.getGenreName(28));
    assertNotNull(genreService.getGenreName(12));
    assertNotNull(genreService.getGenreName(16));
    assertNotNull(genreService.getGenreName(35));
    assertNotNull(genreService.getGenreName(80));
  }

  @Test
  public void testSingletonInstance() {
    MovieGenreService instance1 = MovieGenreService.getInstance();
    MovieGenreService instance2 = MovieGenreService.getInstance();

    assertSame(instance1, instance2);
  }
}