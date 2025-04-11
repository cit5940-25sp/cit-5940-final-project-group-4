package model.game;

import lombok.Data;

/**
 * 胜利条件模型
 */
@Data
public class WinCondition {
    // 条件类型（电影类型、演员、导演等）
    private String conditionType;

    // 条件值（具体类型、演员名、导演名等）
    private String conditionValue;

    // 目标次数（需要多少次达成条件）
    private int targetCount;

    // 当前进度
    private int currentCount;

    /**
     * 构造函数
     */
    public WinCondition(String conditionType, String conditionValue, int targetCount) {
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.targetCount = targetCount;
        this.currentCount = 0;
    }

    /**
     * 构造函数（默认目标次数为1）
     */
    public WinCondition(String conditionType, String conditionValue) {
        this(conditionType, conditionValue, 1);
    }

    /**
     * 增加当前进度
     */
    public void incrementProgress() {
        this.currentCount++;
    }

    /**
     * 检查是否已达成胜利条件
     */
    public boolean isAchieved() {
        return this.currentCount >= this.targetCount;
    }
}
