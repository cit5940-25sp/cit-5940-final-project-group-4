package service.movie;

import lombok.extern.slf4j.Slf4j;
import model.tmdb.Genre;
import service.tmdbApi.TMDBApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电影类型服务
 * 负责管理电影类型的映射关系
 */
@Slf4j
public class MovieGenreService {
    // 单例实例
    private static MovieGenreService instance;
    // 类型ID到名称的映射
    private final Map<Integer, String> genreMap = new ConcurrentHashMap<>();
    // 类型名称到ID的映射
    private final Map<String, Integer> genreNameMap = new ConcurrentHashMap<>();

    /**
     * 私有构造函数
     */
    private MovieGenreService() {
        initializeGenres();
    }

    /**
     * 获取单例实例
     */
    public static synchronized MovieGenreService getInstance() {
        if (instance == null) {
            instance = new MovieGenreService();
        }
        return instance;
    }

    /**
     * 初始化电影类型映射
     */
    private void initializeGenres() {
        List<Genre> genres = TMDBApiService.getMovieGenres();
        if (genres.isEmpty()) {
            log.warn("无法获取电影类型列表，使用默认映射");
            // 使用默认映射
            Map<Integer, String> defaultGenres = new HashMap<>();
            defaultGenres.put(28, "Action");
            defaultGenres.put(12, "Adventure");
            defaultGenres.put(16, "Animation");
            defaultGenres.put(35, "Comedy");
            defaultGenres.put(80, "Crime");
            defaultGenres.put(99, "Documentary");
            defaultGenres.put(18, "Drama");
            defaultGenres.put(10751, "Family");
            defaultGenres.put(14, "Fantasy");
            defaultGenres.put(36, "History");
            defaultGenres.put(27, "Horror");
            defaultGenres.put(10402, "Music");
            defaultGenres.put(9648, "Mystery");
            defaultGenres.put(10749, "Romance");
            defaultGenres.put(878, "Science Fiction");
            defaultGenres.put(10770, "TV Movie");
            defaultGenres.put(53, "Thriller");
            defaultGenres.put(10752, "War");
            defaultGenres.put(37, "Western");

            defaultGenres.forEach((id, name) -> {
                genreMap.put(id, name);
                genreNameMap.put(name, id);
            });
        } else {
            // 使用API返回的类型列表
            genres.forEach(genre -> {
                genreMap.put(genre.getId(), genre.getName());
                genreNameMap.put(genre.getName(), genre.getId());
            });
        }
    }

    /**
     * 获取类型ID对应的名称
     *
     * @param genreId 类型ID
     * @return 类型名称
     */
    public String getGenreName(int genreId) {
        return genreMap.getOrDefault(genreId, "Unknown Genre");
    }

    /**
     * 获取类型名称对应的ID
     *
     * @param genreName 类型名称
     * @return 类型ID
     */
    public Integer getGenreId(String genreName) {
        return genreNameMap.get(genreName);
    }

    /**
     * 检查电影是否属于指定类型
     *
     * @param genreIds  电影的类型ID列表
     * @param genreName 要检查的类型名称
     * @return 是否属于指定类型
     */
    public boolean hasGenre(int[] genreIds, String genreName) {
        if (genreIds == null || genreName == null) {
            return false;
        }
        Integer targetGenreId = getGenreId(genreName);
        if (targetGenreId == null) {
            return false;
        }
        for (int genreId : genreIds) {
            if (genreId == targetGenreId) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all genre ID-to-name mappings.
     *
     * @return an unmodifiable map of genre IDs to genre names
     */
    public Map<Integer, String> getAllGenreMap() {
        return Map.copyOf(genreMap);
    }

    /**
     * Get a sorted list of all genre names.
     *
     * @return sorted list of genre names
     */
    public List<String> getAllGenreNames() {
        return genreMap.values()
                       .stream()
                       .sorted()
                       .toList();
    }

}
