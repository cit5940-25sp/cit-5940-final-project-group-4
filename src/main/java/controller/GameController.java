package controller;

import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.movie.MovieDataService;
//import service.movie.MovieDataServiceImpl;
import service.movie.MovieGenreService;
import view.ConsoleView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.io.IOException;

public class GameController {
    private final GameSession session;
    private final MovieDataService movieDataService;
    private ConsoleView view;

    public GameController(GameSession session, MovieDataService movieDataService) {
        this.session = session;
        this.movieDataService = movieDataService;
    }

    private WinCondition promptUserToCreateWinCondition() {
        Map<Integer, String> genreMap = MovieGenreService.getInstance().getAllGenreMap();
        List<String> genreNames = genreMap.values().stream().sorted().toList();
        return view.promptGenreWinCondition(genreNames, 1, 10);
    }

    public void startGame() {
        try {
            this.view = new ConsoleView();
            view.showWelcome();

            WinCondition condition = promptUserToCreateWinCondition();
            session.setWinCondition(condition);

            if (condition != null) {
                view.showWinCondition(condition.getConditionType() + " = " +
                        condition.getConditionValue() + ", " +
                        condition.getTargetCount() + " times");
            }

            Movie currentMovie = session.getCurrentMovie();
            if (session.getRecentHistory().isEmpty()) {
                session.addInitialMovieToHistory(currentMovie);
            }

            while (!session.hasWon()) {
                String selectedTitle = view.showGameTurn(
                        session.getCurrentStep(),
                        session.getCurrentPlayerName(),
                        session.getRecentHistory(),
                        currentMovie,
                        condition,
                        movieDataService,
                        () -> {
                            try {
                                view.stop();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("\u23F0 Time's up! You lost.");
                            System.exit(0);
                        }
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

                // Ensure full movie data is loaded
                selected = movieDataService.getMovieById(selected.getId());

                if (selected.getGenreIds() == null) {
                    selected = movieDataService.getMovieById(selected.getId());
                }

                MovieCredits credits = movieDataService.getMovieCredits(selected.getId());
                if (credits == null || credits.getCast() == null || credits.getCrew() == null) {
                    credits = movieDataService.getMovieCredits(selected.getId());
                }

                List<String> genreNames = (selected.getGenreIds() == null || selected.getGenreIds().length == 0)
                        ? List.of("Unknown")
                        : Arrays.stream(selected.getGenreIds())
                            .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                            .toList();

//                view.showFullMovieDetails(
//                        selected,
//                        genreNames,
//                        credits.getCrew().stream().filter(c -> "Director".equals(c.getJob())).map(c -> c.getName()).toList(),
//                        credits.getCrew().stream().filter(c -> "Writer".equals(c.getJob()) || "Screenplay".equals(c.getJob())).map(c -> c.getName()).toList(),
//                        credits.getCrew().stream().filter(c -> "Cinematographer".equals(c.getJob()) || "Director of Photography".equals(c.getJob())).map(c -> c.getName()).toList(),
//                        credits.getCrew().stream().filter(c -> "Composer".equals(c.getJob()) || "Original Music Composer".equals(c.getJob())).map(c -> c.getName()).toList(),
//                        credits.getCast().stream().limit(8).map(c -> c.getName()).toList()
//                );

                movieDataService.registerUsedMovie(selected, session);

                List<Connection> connections = movieDataService.getConnections(currentMovie, selected);
                boolean connectionRegistered = false;

                for (Connection conn : connections) {
                    if (!movieDataService.isConnectionUsedThreeTimes(conn, session)) {
                        movieDataService.registerUsedConnection(conn, session);
//                        view.showConnectionInfo(conn.getConnectionValue());
                        session.addToHistory(selected, conn);
                        connectionRegistered = true;
                        break;
                    }
                }

                if (!connectionRegistered) {
                    view.showError("All available connections between the movies have been used 3 times.");
                    continue;
                }

                if (movieDataService.matchesWinCondition(selected, condition)) {
                    condition.incrementProgress();
                }

                // Ensure next currentMovie has full data
                selected = movieDataService.getMovieById(selected.getId());

                currentMovie = selected;
                session.switchTurn();
            }
            
            if(!session.hasWon()) {
                try {
                    view.showVictory();
                    view.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("🎉 You won! You met the win condition!");
                System.exit(0);
            }


        } catch (IOException e) {
            System.err.println("Error initializing Lanterna screen: " + e.getMessage());
        } catch (Exception e) {
            if (view != null) view.showError("Unexpected error: " + e.getMessage());
            else System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}