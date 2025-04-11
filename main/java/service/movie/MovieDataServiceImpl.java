package service.movie;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.tmdbApi.TMDBApiService;
import service.tmdbApi.TMDBMovieCacheService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 电影数据服务实现类
 * 实现游戏所需的所有电影数据相关功能
 */
@Slf4j
public class MovieDataServiceImpl implements MovieDataService {
    // 测试数据电影数量
    private static final int TEST_MOVIES_LIMIT = 20;
    // 单例实例
    private static MovieDataServiceImpl instance;
    /**
     * -- SETTER --
     * 设置测试模式
     */
    // 测试模式标志
    @Setter
    private static boolean testMode = false;
    // 电影索引服务
    private final MovieIndexService indexService;
    // 随机数生成器
    private final Random random = new Random();
    // 初始电影列表（Top 5000）
    private List<Movie> initialMoviesList;
    // 启动器电影列表（每日更新的起始电影）
    private List<Movie> starterMovies;

    /**
     * 私有构造函数
     */
    private MovieDataServiceImpl() {
        this.indexService = MovieIndexService.getInstance();
        // 加载初始电影列表
        loadInitialMovies();
    }

    /**
     * 获取单例实例
     */
    public static synchronized MovieDataServiceImpl getInstance() {
        if (instance == null) {
            instance = new MovieDataServiceImpl();
        }
        return instance;
    }

    /**
     * 加载初始电影列表
     */
    private void loadInitialMovies() {
        // 确定要加载的电影数量
        int moviesCount = testMode ? TEST_MOVIES_LIMIT : 5000;

        // 从缓存服务获取热门电影
        initialMoviesList = TMDBMovieCacheService.getPopularMovies(moviesCount);
        log.info("已加载{}部初始电影", initialMoviesList.size());

        // 初始化电影索引
        indexService.initializeIndexes(initialMoviesList);

        // 预加载启动器电影列表（当前使用前20部电影或全部作为示例）
        int starterLimit = Math.min(20, initialMoviesList.size());
        starterMovies = initialMoviesList
                .stream()
                .limit(starterLimit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Movie> getInitialMoviesList() {
        return Collections.unmodifiableList(initialMoviesList);
    }

    @Override
    public Movie getRandomStarterMovie() {
        if (starterMovies.isEmpty()) {
            // 如果启动器电影为空，返回初始列表中的第一部电影
            return initialMoviesList.get(0);
        }

        // 随机返回一部启动器电影
        int index = random.nextInt(starterMovies.size());
        return starterMovies.get(index);
    }

    @Override
    public List<Movie> searchMoviesByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        // 从索引中搜索
        List<Movie> results = indexService.searchByPrefix(prefix);

        // 如果本地索引没有结果，调用API搜索
        if (results.isEmpty()) {
            results = TMDBApiService.searchMovies(prefix, 1);

            // 添加到索引
            for (Movie movie : results) {
                indexService.getMovieById(movie.getId());
            }
        }

        return results;
    }

    @Override
    public Movie getMovieById(int movieId) {
        return indexService.getMovieById(movieId);
    }

    @Override
    public boolean validateConnection(Movie previousMovie, Movie currentMovie) {
        if (previousMovie == null || currentMovie == null) {
            return false;
        }

        // 获取两部电影之间的连接
        List<Connection> connections = getConnections(previousMovie, currentMovie);

        // 如果有连接，则验证通过
        return !connections.isEmpty();
    }

    @Override
    public List<Connection> getConnections(Movie previousMovie, Movie currentMovie) {
        if (previousMovie == null || currentMovie == null) {
            return Collections.emptyList();
        }

        List<Connection> connections = new ArrayList<>();

        // 获取电影演职人员信息
        MovieCredits previousCredits = indexService.getMovieCredits(previousMovie.getId());
        MovieCredits currentCredits = indexService.getMovieCredits(currentMovie.getId());

        if (previousCredits == null || currentCredits == null) {
            return Collections.emptyList();
        }

        // 检查共同演员
        Map<Integer, CastMember> previousCastMap = previousCredits
                .getCast()
                .stream()
                .collect(Collectors.toMap(CastMember::getId, cast -> cast));

        for (CastMember currentCast : currentCredits.getCast()) {
            if (previousCastMap.containsKey(currentCast.getId())) {
                CastMember previousCast = previousCastMap.get(currentCast.getId());
                connections.add(new Connection(previousMovie, currentMovie, "演员",
                                               previousCast.getName(), previousCast.getId()));
            }
        }

        // 检查共同导演
        Map<Integer, CrewMember> previousDirectorsMap = previousCredits
                .getCrew()
                .stream()
                .filter(crew -> "Director".equals(crew.getJob()))
                .collect(Collectors.toMap(CrewMember::getId, crew -> crew));

        for (CrewMember currentCrew : currentCredits.getCrew()) {
            if ("Director".equals(currentCrew.getJob()) && previousDirectorsMap.containsKey(currentCrew.getId())) {
                CrewMember previousDirector = previousDirectorsMap.get(currentCrew.getId());
                connections.add(new Connection(previousMovie, currentMovie, "导演",
                                               previousDirector.getName(),
                                               previousDirector.getId()));
            }
        }

        // 检查共同编剧
        Map<Integer, CrewMember> previousWritersMap = previousCredits
                .getCrew()
                .stream()
                .filter(crew -> "Writer".equals(crew.getJob()) || "Screenplay".equals(crew.getJob()))
                .collect(Collectors.toMap(CrewMember::getId, crew -> crew, (a, b) -> a));

        for (CrewMember currentCrew : currentCredits.getCrew()) {
            if (("Writer".equals(currentCrew.getJob()) || "Screenplay".equals(currentCrew.getJob())) && previousWritersMap.containsKey(currentCrew.getId())) {
                CrewMember previousWriter = previousWritersMap.get(currentCrew.getId());
                connections.add(new Connection(previousMovie, currentMovie, "编剧",
                                               previousWriter.getName(), previousWriter.getId()));
            }
        }

        return connections;
    }

    @Override
    public boolean isConnectionUsedThreeTimes(Connection connection, GameSession session) {
        return session.isConnectionUsedThreeTimes(connection.getPersonId());
    }

    @Override
    public boolean isMovieAlreadyUsed(Movie movie, GameSession session) {
        return session.isMovieAlreadyUsed(movie);
    }

    @Override
    public boolean matchesWinCondition(Movie movie, WinCondition condition) {
        if (movie == null || condition == null) {
            return false;
        }

        String type = condition.getConditionType();
        String value = condition.getConditionValue();

        switch (type.toLowerCase()) {
            case "genre":
                // 检查电影类型
                return checkMovieGenre(movie, value);

            case "actor":
                // 检查演员
                return checkMovieActor(movie, value);

            case "director":
                // 检查导演
                return checkMovieDirector(movie, value);

            case "writer":
                // 检查编剧
                return checkMovieWriter(movie, value);

            default:
                return false;
        }
    }

    /**
     * 检查电影类型是否匹配
     */
    private boolean checkMovieGenre(Movie movie, String genreName) {
        return MovieGenreService
                .getInstance()
                .hasGenre(movie.getGenreIds(), genreName);
    }

    /**
     * 检查电影演员是否匹配
     */
    private boolean checkMovieActor(Movie movie, String actorName) {
        MovieCredits credits = indexService.getMovieCredits(movie.getId());
        if (credits == null) {
            return false;
        }

        return credits
                .getCast()
                .stream()
                .anyMatch(cast -> cast
                        .getName()
                        .equalsIgnoreCase(actorName));
    }

    /**
     * 检查电影导演是否匹配
     */
    private boolean checkMovieDirector(Movie movie, String directorName) {
        MovieCredits credits = indexService.getMovieCredits(movie.getId());
        if (credits == null) {
            return false;
        }

        return credits
                .getCrew()
                .stream()
                .filter(crew -> "Director".equals(crew.getJob()))
                .anyMatch(director -> director
                        .getName()
                        .equalsIgnoreCase(directorName));
    }

    /**
     * 检查电影编剧是否匹配
     */
    private boolean checkMovieWriter(Movie movie, String writerName) {
        MovieCredits credits = indexService.getMovieCredits(movie.getId());
        if (credits == null) {
            return false;
        }

        return credits
                .getCrew()
                .stream()
                .filter(crew -> "Writer".equals(crew.getJob()) || "Screenplay".equals(crew.getJob()))
                .anyMatch(writer -> writer
                        .getName()
                        .equalsIgnoreCase(writerName));
    }

    @Override
    public void registerUsedMovie(Movie movie, GameSession session) {
        session.registerUsedMovie(movie);
    }

    @Override
    public void registerUsedConnection(Connection connection, GameSession session) {
        session.registerUsedConnection(connection);
    }

    @Override
    public void initializeDataIndexes() {
        // 重新初始化索引，可在需要时调用
        if (initialMoviesList != null && !initialMoviesList.isEmpty()) {
            log.info("重新初始化电影索引...");
            indexService.initializeIndexes(initialMoviesList);
        } else {
            log.warn("初始化索引失败：电影列表为空");
            // 重新加载初始电影列表
            loadInitialMovies();
        }
    }
}
