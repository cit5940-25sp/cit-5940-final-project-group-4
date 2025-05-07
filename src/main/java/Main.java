import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import service.tmdbApi.TMDBApiService;
import service.tmdbApi.TMDBMovieService;
import controller.GameController;
import model.game.GameSession;
import model.game.WinCondition;
import service.movie.MovieDataService;
import service.movie.MovieDataServiceImpl;

import java.util.List;

@Slf4j
public class Main {
    public static void main(String[] args) {
        // Step 1: Get movie data service (singleton)
        MovieDataService movieDataService = MovieDataServiceImpl.getInstance();

        // Step 2: Pick a starting movie (e.g., "Inception")
//        Movie startMovie = movieDataService.searchMoviesByPrefix("Inception").stream()
//                                           .findFirst()
//                                           .orElseThrow(() -> new RuntimeException("Start movie not found"));
        
        Movie startMovie = movieDataService.getRandomStarterMovie();
        if (startMovie == null) {
            throw new RuntimeException("No starter movie available.");
        }

        // Step 3: Generate two random win conditions (handled inside GameController, so pass placeholders here)
        WinCondition player1Condition = new WinCondition("genre", "Action", 3);  // placeholder
        WinCondition player2Condition = new WinCondition("genre", "Comedy", 4);  // placeholder

        // Step 4: Create a game session with both conditions
        GameSession session = new GameSession("session-001", startMovie, player1Condition, player2Condition, "Player1", "Player2");

        // Step 5: Create controller and start game
        GameController controller = new GameController(session, movieDataService);
        controller.startGame();
    }
}
