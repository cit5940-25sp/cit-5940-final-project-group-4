package controller;

import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.Movie;
import service.movie.MovieDataService;
import service.movie.MovieDataServiceImpl;

import java.util.List;
import java.util.Scanner;

public class GameController {
    private final MovieDataService movieService;
    private GameSession session;
    private WinCondition winCondition;
    private final Scanner scanner;

    public GameController() {
        this.movieService = MovieDataServiceImpl.getInstance();
        this.scanner = new Scanner(System.in);
    }

    public void startGame() {
        System.out.println("\nWelcome to the Movie Battle Game!");

        // Initialize data indexes
        movieService.initializeDataIndexes();

        // Get starting movie
//        Movie startMovie = movieService.getRandomStarterMovie();
        // For testing:
        Movie startMovie = movieService.searchMoviesByPrefix("Inception").get(0);
        System.out.println("\nStarting movie: " + startMovie.getTitle());

        // Initialize game session
        session = new GameSession("session-1", startMovie);

        // Set win condition (e.g., 3 Horror movies)
        winCondition = new WinCondition("genre", "Horror", 3);
        System.out.println("Win condition: Find 3 Horror movies\n");

        while (true) {
            System.out.println("Current movie: " + session.getCurrentMovie().getTitle());
            System.out.print("Enter the prefix of the next movie: ");
            String prefix = scanner.nextLine();

            List<Movie> suggestions = movieService.searchMoviesByPrefix(prefix);
            if (suggestions.isEmpty()) {
                System.out.println("No matching movies found. Please try again!");
                continue;
            }

            System.out.println("Suggested movies:");
            for (int i = 0; i < suggestions.size(); i++) {
                System.out.println((i + 1) + ". " + suggestions.get(i).getTitle());
            }

            System.out.print("Choose the movie number: ");
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine()) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number!\n");
                continue;
            }

            if (choice < 0 || choice >= suggestions.size()) {
                System.out.println("Selection out of range. Please try again!\n");
                continue;
            }

            Movie nextMovie = suggestions.get(choice);

            if (movieService.isMovieAlreadyUsed(nextMovie, session)) {
                System.out.println("This movie has already been used. Choose another one!\n");
                continue;
            }

            if (!movieService.validateConnection(session.getCurrentMovie(), nextMovie)) {
                System.out.println("No valid connection between the two movies. Try again!\n");
                continue;
            }

            List<Connection> connections = movieService.getConnections(session.getCurrentMovie(), nextMovie);
            Connection firstValid = null;
            for (Connection c : connections) {
                if (!movieService.isConnectionUsedThreeTimes(c, session)) {
                    firstValid = c;
                    break;
                }
            }
            if (firstValid == null) {
                System.out.println("All connection points have been used 3 times. Try another movie!\n");
                continue;
            }

            // Register connection and movie
            movieService.registerUsedConnection(firstValid, session);
            movieService.registerUsedMovie(nextMovie, session);

            if (movieService.matchesWinCondition(nextMovie, winCondition)) {
                winCondition.incrementProgress();
                System.out.println("This movie matches the win condition! Progress: " + winCondition.getCurrentCount() + "/" + winCondition.getTargetCount());
            }

            if (winCondition.isAchieved()) {
                System.out.println("\nCongratulations! You've achieved the win condition. Game over!");
                break;
            }

            System.out.println("-----------------------------\n");
        }
    }
}
