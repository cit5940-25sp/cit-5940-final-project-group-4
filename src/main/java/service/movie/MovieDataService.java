package service.movie;

import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;

import java.util.List;

/**
 * 电影数据服务接口
 * 提供游戏所需的所有电影数据相关功能
 */
public interface MovieDataService {
    /**
     * 获取初始电影列表
     *
     * @return Top 5000 热门电影列表
     */
    List<Movie> getInitialMoviesList();

    /**
     * 获取随机起始电影
     *
     * @return 一部随机选择的起始电影
     */
    Movie getRandomStarterMovie();

    /**
     * 根据前缀搜索电影
     *
     * @param prefix 电影名称前缀
     * @return 匹配的电影列表
     */
    List<Movie> searchMoviesByPrefix(String prefix);

    /**
     * 根据ID获取电影详情
     *
     * @param movieId 电影ID
     * @return 电影详情
     */
    Movie getMovieById(int movieId);

    /**
     * 验证两部电影之间是否存在有效连接
     *
     * @param previousMovie 前一部电影
     * @param currentMovie  当前电影
     * @return 是否存在有效连接
     */
    boolean validateConnection(Movie previousMovie, Movie currentMovie);

    /**
     * 获取两部电影之间的所有连接
     *
     * @param previousMovie 前一部电影
     * @param currentMovie  当前电影
     * @return 连接列表
     */
    List<Connection> getConnections(Movie previousMovie, Movie currentMovie);

    /**
     * 检查连接是否已被使用三次
     *
     * @param connection 连接
     * @param session    游戏会话
     * @return 是否已使用三次
     */
    boolean isConnectionUsedThreeTimes(Connection connection, GameSession session);

    /**
     * 检查电影是否已在游戏中使用
     *
     * @param movie   电影
     * @param session 游戏会话
     * @return 是否已使用
     */
    boolean isMovieAlreadyUsed(Movie movie, GameSession session);

    /**
     * 检查电影是否满足胜利条件
     *
     * @param movie     电影
     * @param condition 胜利条件
     * @return 是否满足条件
     */
    boolean matchesWinCondition(Movie movie, WinCondition condition);

    /**
     * 注册已使用的电影
     *
     * @param movie   电影
     * @param session 游戏会话
     */
    void registerUsedMovie(Movie movie, GameSession session);

    /**
     * 注册已使用的连接
     *
     * @param connection 连接
     * @param session    游戏会话
     */
    void registerUsedConnection(Connection connection, GameSession session);

    /**
     * 初始化数据索引
     */
    void initializeDataIndexes();
    
    MovieCredits getMovieCredits(int movieId);

}
