package model.game;

import lombok.Data;
import model.tmdb.Movie;
// Edit: Import WinCondition
import model.game.WinCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
    
    // Edit: Win condition for the current game
    private WinCondition winCondition;
    
    private String player1Name;
    private String player2Name;
    private boolean isPlayer1Turn;
    
    private final List<HistoryRecord> recentHistory = new LinkedList<>();


    /**
     * 构造函数
     */
    public GameSession(String sessionId, Movie startMovie, WinCondition winCondition, String player1Name, String player2Name) {
        this.sessionId = sessionId;
        this.currentMovie = startMovie;
        this.usedMovies = new ArrayList<>();
        this.usedMovies.add(startMovie);
        this.usedConnections = new ArrayList<>();
        this.connectionUsageCount = new HashMap<>();
        this.currentStep = 1;
        this.inSetupPhase = true;
        this.winCondition = winCondition;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.isPlayer1Turn = true;
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
    
    public WinCondition getWinCondition() {
        return this.winCondition;
    }
    
    public boolean hasWon() {
        return winCondition != null && winCondition.isAchieved();
    }
    
    public String getCurrentPlayerName() {
        return isPlayer1Turn ? player1Name : player2Name;
    }

    public void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
    }
    
    public void addInitialMovieToHistory(Movie movie) {
        recentHistory.add(new HistoryRecord(movie, null));
    }
    
    public void addToHistory(Movie movie, Connection connection) {
        if (recentHistory.size() == 5) {
            recentHistory.remove(0);  // Keep at most 5 history records
        }
        recentHistory.add(new HistoryRecord(movie, connection));
    }

    public List<HistoryRecord> getRecentHistory() {
        return new ArrayList<>(recentHistory);
    }



}
