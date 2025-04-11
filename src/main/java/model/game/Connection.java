package model.game;

import lombok.Data;
import model.tmdb.Movie;

/**
 * 电影连接模型
 * 表示两部电影之间的共通关系（演员、导演等）
 */
@Data
public class Connection {
    // 连接的两部电影
    private Movie movie1;
    private Movie movie2;

    // 连接类型（演员、导演、编剧等）
    private String connectionType;

    // 连接值（演员名、导演名等）
    private String connectionValue;

    // 连接使用次数
    private int usageCount;

    // 连接人员ID
    private int personId;

    /**
     * 构造函数
     */
    public Connection(Movie movie1, Movie movie2, String connectionType, String connectionValue,
                      int personId) {
        this.movie1 = movie1;
        this.movie2 = movie2;
        this.connectionType = connectionType;
        this.connectionValue = connectionValue;
        this.personId = personId;
        this.usageCount = 0;
    }

    /**
     * 增加使用次数
     */
    public void incrementUsage() {
        this.usageCount++;
    }

    /**
     * 检查是否已达到最大使用次数（3次）
     */
    public boolean isMaxUsed() {
        return this.usageCount >= 3;
    }
}
