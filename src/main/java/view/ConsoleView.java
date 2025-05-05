package view;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import model.game.Connection;
import model.game.HistoryRecord;
import model.game.WinCondition;
import model.tmdb.Movie;
import service.movie.MovieGenreService;

public class ConsoleView {
    private final Scanner scanner = new Scanner(System.in);

    public void showWelcome() {
        System.out.println("\nWelcome to the Movie Name Game!");
    }
    

    public void showWinCondition(String condition) {
        System.out.println("Win condition: " + condition);
    }
    
    public void showCurrentRound(int step) {
        System.out.println("\n=== Round " + step + " ===");
    }
    
    public void showCurrentPlayer(String playerName) {
        System.out.println("\n>>> Now it's " + playerName + "'s turn <<<");
    }


    public WinCondition promptGenreWinCondition(List<String> genreNames, int min, int max) {
        System.out.println("Choose a genre for your win condition:");
        for (int i = 0; i < genreNames.size(); i++) {
            System.out.println((i + 1) + ". " + genreNames.get(i));
        }

        int genreIndex = -1;
        while (true) {
            try {
                System.out.print("Enter the number of your chosen genre: ");
                genreIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
                if (genreIndex >= 0 && genreIndex < genreNames.size()) break;
                else System.out.println("Please enter a number between 1 and " + genreNames.size());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        String genre = genreNames.get(genreIndex);

        int count = -1;
        while (true) {
            try {
                System.out.print("How many times should a movie match genre \"" + genre + "\" to win? (" + min + "-" + max + "): ");
                count = Integer.parseInt(scanner.nextLine().trim());
                if (count >= min && count <= max) break;
                else System.out.println("Please enter a number between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        return new WinCondition("genre", genre, count);
    }


    
    public void showFullMovieDetails(Movie movie, List<String> genres,
            List<String> directors, List<String> writers,
            List<String> cinematographers, List<String> composers,
            List<String> castMembers) {
        System.out.println();
        System.out.println("Title: " + movie.getTitle() + " (" + movie.getReleaseDate().split("-")[0] + ")");
        System.out.println("Genres: " + String.join(", ", genres));
        
        if (!directors.isEmpty()) System.out.println("Director: " + String.join(", ", directors));
        if (!writers.isEmpty()) System.out.println("Writers: " + String.join(", ", writers));
        if (!cinematographers.isEmpty()) System.out.println("Cinematographer: " + String.join(", ", cinematographers));
        if (!composers.isEmpty()) System.out.println("Composer: " + String.join(", ", composers));
        if (!castMembers.isEmpty()) System.out.println("Featuring: " + String.join(", ", castMembers));
    }


    public String promptMoviePrefix() {
        System.out.print("Enter the prefix of the next movie: ");
        return scanner.nextLine().trim();
    }

    public void showSuggestions(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) {
            System.out.println("No suggestions found for this prefix.");
            return;
        }
        System.out.println("Suggested movies:");
        for (int i = 0; i < movies.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, movies.get(i).getTitle());
        }
    }

    public int promptMovieChoice(int max) {
        while (true) {
            try {
                System.out.print("Choose the movie number: ");
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= max) return choice - 1;
                else System.out.println("Invalid number. Try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
    

    public void showConnectionInfo(String connectionValue) {
        System.out.println("This Connection is valid: " + connectionValue);
    }
    
    public void showRecentHistory(List<HistoryRecord> history) {
        System.out.println("── Recent History (last 5 turns) ──");

        if (history.isEmpty()) {
            System.out.println("No history yet.");
            return;
        }

        int start = Math.max(0, history.size() - 5);

        for (int i = start; i < history.size(); i++) {
            HistoryRecord record = history.get(i);
            Movie movie = record.getMovie();

            String[] genreNames;
            int[] genreIds = movie.getGenreIds();
            if (genreIds == null || genreIds.length == 0) {
                genreNames = new String[]{"Unknown"};
            } else {
                genreNames = Arrays.stream(genreIds)
                        .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                        .toArray(String[]::new);
            }

            if (i > start) {
                Connection conn = record.getConnection();
                if (conn != null) {
                    System.out.println("      |");
                    System.out.println("Connection: " + conn.getConnectionValue());
                    System.out.println("      |");
                }
            }

            System.out.printf("▶ %s (%s), Genres: %s%n",
                    movie.getTitle(),
                    movie.getReleaseDate().split("-")[0],
                    String.join(", ", genreNames)
            );
                        
        }
    }


    public void showProgress(int current, int target) {
        System.out.println("Progress: " + current + " / " + target + " movies matched the win condition.");
    }

    public void showVictory() {
        System.out.println("\nYou won! You met the win condition!");
    }

    public void showError(String message) {
        System.out.println("Error: " + message);
    }
}
