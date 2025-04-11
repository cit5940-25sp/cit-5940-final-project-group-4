package model.game;

import lombok.Data;
import model.tmdb.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏会话模型
 */
@Data
public class GameSession {
    // 会话ID
    private String sessionId;

    // 已使用的电影列表
    private List<Movie> usedMovies;

    // 已使用的连接列表（包括使用次数）
    private List<Connection> usedConnections;

    // 连接使用计数（根据personId）
    private Map<Integer, Integer> connectionUsageCount;

    // 当前电影
    private Movie currentMovie;

    // 当前游戏步数
    private int currentStep;

    // 设置阶段（前3步）标志
    private boolean inSetupPhase;

    /**
     * 构造函数
     */
    public GameSession(String sessionId, Movie startMovie) {
        this.sessionId = sessionId;
        this.currentMovie = startMovie;
        this.usedMovies = new ArrayList<>();
        this.usedMovies.add(startMovie);
        this.usedConnections = new ArrayList<>();
        this.connectionUsageCount = new HashMap<>();
        this.currentStep = 1;
        this.inSetupPhase = true;
    }

    /**
     * 注册使用的电影
     */
    public void registerUsedMovie(Movie movie) {
        if (!isMovieAlreadyUsed(movie)) {
            this.usedMovies.add(movie);
        }
        this.currentMovie = movie;
        this.currentStep++;

        // 检查是否超过设置阶段（3步）
        if (this.currentStep > 3) {
            this.inSetupPhase = false;
        }
    }

    /**
     * 注册使用的连接
     */
    public void registerUsedConnection(Connection connection) {
        // 增加连接使用次数
        connection.incrementUsage();
        this.usedConnections.add(connection);

        // 更新personId使用计数
        int personId = connection.getPersonId();
        int count = this.connectionUsageCount.getOrDefault(personId, 0);
        this.connectionUsageCount.put(personId, count + 1);
    }

    /**
     * 检查电影是否已使用
     */
    public boolean isMovieAlreadyUsed(Movie movie) {
        return usedMovies
                .stream()
                .anyMatch(m -> m.getId() == movie.getId());
    }

    /**
     * 检查连接是否已使用三次
     */
    public boolean isConnectionUsedThreeTimes(int personId) {
        return this.connectionUsageCount.getOrDefault(personId, 0) >= 3;
    }
}
