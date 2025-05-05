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

public class GameController {
    private final GameSession session;
    private final MovieDataService movieDataService;
    private final ConsoleView view;

    public GameController(GameSession session, MovieDataService movieDataService) {
        this.session = session;
        this.movieDataService = movieDataService;
        this.view = new ConsoleView();
    }
    
    private WinCondition promptUserToCreateWinCondition() {
        Map<Integer, String> genreMap = MovieGenreService.getInstance().getAllGenreMap();
        List<String> genreNames = genreMap.values().stream().sorted().toList();
        return view.promptGenreWinCondition(genreNames, 1, 10);
    }



    public void startGame() {
        view.showWelcome();

     // Let user define win condition
        WinCondition condition = promptUserToCreateWinCondition();
        session.setWinCondition(condition);

        // Display win condition
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
            view.showCurrentRound(session.getCurrentStep());
            view.showCurrentPlayer(session.getCurrentPlayerName());
            
            view.showRecentHistory(session.getRecentHistory());
            
            int[] genreIds = currentMovie.getGenreIds();
            List<String> genreNames = Arrays.stream(genreIds)
                .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                .toList();

            MovieCredits credits = movieDataService.getMovieCredits(currentMovie.getId());

            List<String> directors = credits.getCrew().stream()
                .filter(c -> "Director".equals(c.getJob()))
                .map(c -> c.getName())
                .toList();

            List<String> writers = credits.getCrew().stream()
                .filter(c -> "Writer".equals(c.getJob()) || "Screenplay".equals(c.getJob()))
                .map(c -> c.getName())
                .toList();

            List<String> cinematographers = credits.getCrew().stream()
                .filter(c -> "Director of Photography".equals(c.getJob()) || "Cinematographer".equals(c.getJob()))
                .map(c -> c.getName())
                .toList();

            List<String> composers = credits.getCrew().stream()
                .filter(c -> "Composer".equals(c.getJob()) || "Original Music Composer".equals(c.getJob()))
                .map(c -> c.getName())
                .toList();

            List<String> castMembers = credits.getCast().stream()
                .limit(8)
                .map(c -> c.getName())
                .toList();

            view.showFullMovieDetails(currentMovie, genreNames, directors, writers, cinematographers, composers, castMembers);
            view.showProgress(condition.getCurrentCount(), condition.getTargetCount());
            
            String prefix = view.promptMoviePrefix();
            List<Movie> suggestions = movieDataService.searchMoviesByPrefix(prefix);
            view.showSuggestions(suggestions);

            if (suggestions.isEmpty()) continue;

            int index = view.promptMovieChoice(suggestions.size());
            Movie selected = suggestions.get(index);

            // Check for duplicates
            if (movieDataService.isMovieAlreadyUsed(selected, session)) {
                view.showError("You already used this movie.");
                continue;
            }

            // Validate connection
            if (!movieDataService.validateConnection(currentMovie, selected)) {
                view.showError("No valid connection between movies.");
                continue;
            }

            // Register movie
            movieDataService.registerUsedMovie(selected, session);

            // Register connection and show explanation
            List<Connection> connections = movieDataService.getConnections(currentMovie, selected);
            boolean connectionRegistered = false;
            for (Connection conn : connections) {
                if (!movieDataService.isConnectionUsedThreeTimes(conn, session)) {
                    movieDataService.registerUsedConnection(conn, session);
                    view.showConnectionInfo(conn.getConnectionValue());
                    session.addToHistory(selected, conn);
                    connectionRegistered = true;
                    break;
                }
            }
            

            if (!connectionRegistered) {
                view.showError("All available connections between the movies have been used 3 times.");
                continue; // force player to pick another movie
            }

            // Check win condition
            if (movieDataService.matchesWinCondition(selected, condition)) {
                condition.incrementProgress();
            }

            currentMovie = selected;
            session.switchTurn();
        }

        view.showVictory();
    }
}
