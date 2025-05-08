package controller;

import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.movie.MovieDataService;
import service.movie.MovieGenreService;
import view.ConsoleView;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.googlecode.lanterna.gui2.Label;

import java.io.IOException;

public class GameController {
    private final GameSession session;
    private final MovieDataService movieDataService;
    private ConsoleView view;

    public GameController(GameSession session, MovieDataService movieDataService) {
        this.session = session;
        this.movieDataService = movieDataService;
    }

    private WinCondition getRandomGenreWinConditiontion(int targetc) {
        Map<Integer, String> genreMap = MovieGenreService.getInstance().getAllGenreMap();
        List<String> genreNames = genreMap.values().stream().sorted().toList();
        String randomGenre = genreNames.get(new Random().nextInt(genreNames.size()));
        return new WinCondition("genre", randomGenre, targetc);
    }

    public void startGame() {
        try {
            this.view = new ConsoleView();
            view.showWelcome();
            
            int targetCount = new Random().nextInt(5) + 3;

            WinCondition player1Condition = getRandomGenreWinConditiontion(targetCount);
            WinCondition player2Condition = getRandomGenreWinConditiontion(targetCount);
            session.setPlayer1WinCondition(player1Condition);
            session.setPlayer2WinCondition(player2Condition);

            view.showWinCondition(session.getPlayer1Name() + ": " + player1Condition.getConditionValue() + ", " + player1Condition.getTargetCount() + " times");
            view.showWinCondition(session.getPlayer2Name() + ": " + player2Condition.getConditionValue() + ", " + player2Condition.getTargetCount() + " times");

            Movie currentMovie = session.getCurrentMovie();
            if (session.getRecentHistory().isEmpty()) {
                session.addInitialMovieToHistory(currentMovie);
            }
            
            boolean firstAttempt = true;
            Label timerLabel = new Label("Time left: 30s");

            while (!session.hasWon()) {
                String selectedTitle = view.showGameTurn(
                        session.getCurrentStep(),
                        session.getCurrentPlayerName(),
                        session.getRecentHistory(),
                        currentMovie,
                        session.getCurrentPlayerWinCondition(),
                        movieDataService,
                        () -> {
                            try {
                                view.stop();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("\u23F0 Time's up! " + session.getCurrentPlayerName() + " lost the game.");
                            System.exit(0);
                        },
                        timerLabel,
                        firstAttempt
                );
                
                
                List<Movie> candidates = movieDataService.searchMoviesByPrefix(selectedTitle);
                Movie selected = candidates.stream()
                        .filter(m -> m.getTitle().equals(selectedTitle))
                        .findFirst()
                        .orElse(null);

                if (selected == null) {
                    view.showErrorNonBlocking("Movie not found or selection was invalid.");
                    continue;
                }

                if (movieDataService.isMovieAlreadyUsed(selected, session)) {
                    view.showErrorNonBlocking("You already used this movie.");
                    continue;
                }

                if (!movieDataService.validateConnection(currentMovie, selected)) {
                    view.showErrorNonBlocking("No valid connection between movies.");
                    continue;
                }
                
                timerLabel = new Label("Time left: 30s");
                view.resettime();
                

                // Ensure full movie data is loaded
                selected = movieDataService.getMovieById(selected.getId());

                if (selected.getGenreIds() == null) {
                    selected = movieDataService.getMovieById(selected.getId());
                }

                MovieCredits credits = movieDataService.getMovieCredits(selected.getId());
                if (credits == null || credits.getCast() == null || credits.getCrew() == null) {
                    credits = movieDataService.getMovieCredits(selected.getId());
                }


                movieDataService.registerUsedMovie(selected, session);

                List<Connection> connections = movieDataService.getConnections(currentMovie, selected);
                boolean connectionRegistered = false;

                for (Connection conn : connections) {
                    if (!movieDataService.isConnectionUsedThreeTimes(conn, session)) {
                        movieDataService.registerUsedConnection(conn, session);
                        session.addToHistory(selected, conn);
                        connectionRegistered = true;
                        break;
                    }
                }

                if (!connectionRegistered) {
                    view.showError("All available connections between the movies have been used 3 times.");
                    continue;
                }

                WinCondition condition = session.getCurrentPlayerWinCondition();
                if (movieDataService.matchesWinCondition(selected, condition)) {
                    condition.incrementProgress();
                }

                // Ensure next currentMovie has full data
                selected = movieDataService.getMovieById(selected.getId());

                currentMovie = selected;
                session.switchTurn();
                firstAttempt = true;
            }
            session.switchTurn();
            view.stop();
            System.out.println("\uD83C\uDF89" + session.getCurrentPlayerName() +" " +"won! You met the win condition!");
            System.out.flush();
            System.exit(0);

        } catch (IOException e) {
            System.err.println("Error initializing Lanterna screen: " + e.getMessage());
        } catch (Exception e) {
            if (view != null) view.showError("Unexpected error: " + e.getMessage());
            else System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
