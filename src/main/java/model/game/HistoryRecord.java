package model.game;

import model.tmdb.Movie;

/**
 * Represents a single move in the game, including the movie played
 * and the connection to the previous movie (if any).
 * Used for displaying recent turn history.
 */
public class HistoryRecord {
    private final Movie movie;
    private final Connection connection;

    /**
     * Constructs a new HistoryRecord with the given movie and connection.
     *
     * @param movie      the movie selected in this turn
     * @param connection the connection used to link this movie to the previous one;
     *                   can be null if this is the first movie of the game
     */
    public HistoryRecord(Movie movie, Connection connection) {
        this.movie = movie;
        this.connection = connection;
    }

    /**
     * Returns the movie selected in this turn.
     */
    public Movie getMovie() {
        return movie;
    }

    /**
     * Returns the connection used to link this movie to the previous one.
     */
    public Connection getConnection() {
        return connection;
    }
}
