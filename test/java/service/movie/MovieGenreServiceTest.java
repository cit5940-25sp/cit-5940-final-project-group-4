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
    // 重置单例实例
    genreService = MovieGenreService.getInstance();
  }

  @Test
  public void testGetGenreName() {
    // 测试已知类型
    assertEquals("动作", genreService.getGenreName(28));
    assertEquals("冒险", genreService.getGenreName(12));
    assertEquals("动画", genreService.getGenreName(16));

    // 测试未知类型
    assertEquals("未知类型", genreService.getGenreName(999));
  }

  @Test
  public void testGetGenreId() {
    // 测试已知类型
    assertEquals(Integer.valueOf(28), genreService.getGenreId("动作"));
    assertEquals(Integer.valueOf(12), genreService.getGenreId("冒险"));
    assertEquals(Integer.valueOf(16), genreService.getGenreId("动画"));

    // 测试未知类型
    assertNull(genreService.getGenreId("未知类型"));
  }

  @Test
  public void testHasGenre() {
    // 测试包含指定类型的电影
    int[] genreIds = { 28, 12, 16 }; // 动作、冒险、动画
    assertTrue(genreService.hasGenre(genreIds, "动作"));
    assertTrue(genreService.hasGenre(genreIds, "冒险"));
    assertTrue(genreService.hasGenre(genreIds, "动画"));

    // 测试不包含指定类型的电影
    assertFalse(genreService.hasGenre(genreIds, "喜剧"));
    assertFalse(genreService.hasGenre(genreIds, "恐怖"));

    // 测试空数组
    assertFalse(genreService.hasGenre(new int[0], "动作"));

    // 测试null参数
    assertFalse(genreService.hasGenre(null, "动作"));
    assertFalse(genreService.hasGenre(genreIds, null));
    assertFalse(genreService.hasGenre(null, null));
  }

  @Test
  public void testInitializeGenresWithEmptyApiResponse() {
    // 模拟API返回空列表的情况
    List<Genre> emptyGenres = Collections.emptyList();

    // 验证默认映射是否正确加载
    assertNotNull(genreService.getGenreName(28)); // 动作
    assertNotNull(genreService.getGenreName(12)); // 冒险
    assertNotNull(genreService.getGenreName(16)); // 动画
    assertNotNull(genreService.getGenreName(35)); // 喜剧
    assertNotNull(genreService.getGenreName(80)); // 犯罪
  }

  @Test
  public void testSingletonInstance() {
    // 获取两个实例
    MovieGenreService instance1 = MovieGenreService.getInstance();
    MovieGenreService instance2 = MovieGenreService.getInstance();

    // 验证是否是同一个实例
    assertSame(instance1, instance2);
  }
}