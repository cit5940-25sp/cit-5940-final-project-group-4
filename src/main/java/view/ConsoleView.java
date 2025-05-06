package view;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.concurrent.*;
import model.game.Connection;
import model.game.HistoryRecord;
import model.game.WinCondition;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.movie.MovieDataService;
import service.movie.MovieGenreService;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import model.game.WinCondition;
import model.tmdb.Movie;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class ConsoleView {
    private final MultiWindowTextGUI gui;
    private ScheduledExecutorService scheduler;
    private volatile boolean timerRunning;
    private volatile int secondsRemaining;

    public ConsoleView() throws IOException {
        Screen screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();
        gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace());
    }

    public void showWelcome() {
        showMessage("Welcome to the Movie Name Game!");
    }

    public void showWinCondition(String condition) {
        showMessage("Win condition: " + condition);
    }

    public void showCurrentRound(int step) {
        showMessage("=== Round " + step + " ===");
    }

    public void showCurrentPlayer(String playerName) {
        showMessage(">>> Now it's " + playerName + "'s turn <<<");
    }

    private void showMessage(String message) {
        BasicWindow window = new BasicWindow();
        Panel contentPanel = new Panel();
        contentPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        contentPanel.addComponent(new Label(message));
        contentPanel.addComponent(new Button("OK", window::close));
        window.setComponent(contentPanel);
        gui.addWindowAndWait(window);
    }

    public String promptMoviePrefix() {
        return promptTextInput("Enter the prefix of the next movie:");
    }

    public int promptMovieChoice(int max, List<String> titles) {
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        AtomicReference<Integer> selectedIndex = new AtomicReference<>(-1);
        BasicWindow window = new BasicWindow("Movie Suggestions");

        for (int i = 0; i < titles.size(); i++) {
            int index = i;
            panel.addComponent(new Button((i + 1) + ". " + titles.get(i), () -> {
                selectedIndex.set(index);
                window.close();
            }));
        }

        window.setComponent(panel);
        gui.addWindowAndWait(window);
        return selectedIndex.get();
    }

    public WinCondition promptGenreWinCondition(List<String> genreNames, int min, int max) {
        int genreIndex = promptChoice("Choose a genre for your win condition:", genreNames);
        String genre = genreNames.get(genreIndex);
        int count = Integer.parseInt(promptTextInput("How many times should a movie match genre \"" + genre + "\" to win? (" + min + "-" + max + "):"));
        return new WinCondition("genre", genre, count);
    }

    private int promptChoice(String message, List<String> options) {
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        BasicWindow window = new BasicWindow("Choose Option");

        AtomicReference<Integer> selectedIndex = new AtomicReference<>(-1);

        panel.addComponent(new Label(message));
        for (int i = 0; i < options.size(); i++) {
            int index = i;
            panel.addComponent(new Button((i + 1) + ". " + options.get(i), () -> {
                selectedIndex.set(index);
                window.close();
            }));
        }

        window.setComponent(panel);
        gui.addWindowAndWait(window);
        return selectedIndex.get();
    }

    private String promptTextInput(String prompt) {
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        BasicWindow window = new BasicWindow("Input");

        panel.addComponent(new Label(prompt));
        TextBox inputBox = new TextBox().setPreferredSize(new TerminalSize(40, 1));
        panel.addComponent(inputBox);
        panel.addComponent(new Button("Submit", window::close));

        window.setComponent(panel);
        gui.addWindowAndWait(window);

        return inputBox.getText();
    }

    public void stop() throws IOException {
        gui.getScreen().stopScreen();
    }
    
//    public void showFullMovieDetails(Movie movie, List<String> genres,
//            List<String> directors, List<String> writers,
//            List<String> cinematographers, List<String> composers,
//            List<String> castMembers) {
//        BasicWindow window = new BasicWindow("Movie Details");
//        Panel panel = new Panel();
//        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
//        
//        panel.addComponent(new Label("Title: " + movie.getTitle() + " (" + movie.getReleaseDate().split("-")[0] + ")"));
//        panel.addComponent(new Label("Genres: " + String.join(", ", genres)));
//        
//        if (!directors.isEmpty())
//        panel.addComponent(new Label("Director: " + String.join(", ", directors)));
//        if (!writers.isEmpty())
//        panel.addComponent(new Label("Writers: " + String.join(", ", writers)));
//        if (!cinematographers.isEmpty())
//        panel.addComponent(new Label("Cinematographer: " + String.join(", ", cinematographers)));
//        if (!composers.isEmpty())
//        panel.addComponent(new Label("Composer: " + String.join(", ", composers)));
//        if (!castMembers.isEmpty())
//        panel.addComponent(new Label("Featuring: " + String.join(", ", castMembers)));
//        
//        panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
//        panel.addComponent(new Button("OK", window::close));
//        
//        window.setComponent(panel);
//        gui.addWindowAndWait(window);
//    }
    public void showFullMovieDetails(Movie movie, List<String> genres,
            List<String> directors, List<String> writers,
            List<String> cinematographers, List<String> composers,
            List<String> castMembers) {
            BasicWindow window = new BasicWindow("Movie Details");
            Panel panel = new Panel();
            panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

            String title = movie.getTitle();
            String year = (movie.getReleaseDate() != null && movie.getReleaseDate().contains("-")) ? movie.getReleaseDate().split("-")[0] : "Unknown";

            panel.addComponent(new Label("Title: " + title + " (" + year + ")"));
            panel.addComponent(new Label("Genres: " + String.join(", ", genres)));
            
            if (directors != null && !directors.isEmpty())
            panel.addComponent(new Label("Director: " + String.join(", ", directors)));
            if (writers != null && !writers.isEmpty())
            panel.addComponent(new Label("Writers: " + String.join(", ", writers)));
            if (cinematographers != null && !cinematographers.isEmpty())
            panel.addComponent(new Label("Cinematographer: " + String.join(", ", cinematographers)));
            if (composers != null && !composers.isEmpty())
            panel.addComponent(new Label("Composer: " + String.join(", ", composers)));
            if (castMembers != null && !castMembers.isEmpty())
            panel.addComponent(new Label("Featuring: " + String.join(", ", castMembers)));
            
            panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
            panel.addComponent(new Button("OK", window::close));
            
            window.setComponent(panel);
            gui.addWindowAndWait(window);
}

    
    
    public void showProgress(int current, int target) {
        showMessage("Progress: " + current + " / " + target + " movies matched the win condition.");
    }
    
    public void showSuggestions(List<Movie> movies) {
        BasicWindow window = new BasicWindow("Movie Suggestions");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        if (movies == null || movies.isEmpty()) {
            panel.addComponent(new Label("No suggestions found for this prefix."));
        } else {
            panel.addComponent(new Label("Suggested movies:"));
            for (int i = 0; i < movies.size(); i++) {
                panel.addComponent(new Label((i + 1) + ". " + movies.get(i).getTitle()));
            }
        }

        panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        panel.addComponent(new Button("OK", window::close));

        window.setComponent(panel);
        gui.addWindowAndWait(window);
    }
    
    public void showError(String message) {
        BasicWindow window = new BasicWindow("Error");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Error: " + message));
        panel.addComponent(new Button("OK", window::close));

        window.setComponent(panel);
        gui.addWindowAndWait(window);
    }
    
    
    public void showConnectionInfo(String connectionValue) {
        showMessage("This Connection is valid: " + connectionValue);
    }
    
    
    public void showVictory() {
        showMessage("🎉 You won! You met the win condition!");
    }
    
    public void showRecentHistory(List<HistoryRecord> history) {
        BasicWindow window = new BasicWindow("Recent History");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("── Recent History (last 5 turns) ──"));

        if (history.isEmpty()) {
            panel.addComponent(new Label("No history yet."));
        } else {
            int start = Math.max(0, history.size() - 5);

            for (int i = start; i < history.size(); i++) {
                HistoryRecord record = history.get(i);
                Movie movie = record.getMovie();

                // Safe release year
                String year = "Unknown";
                String releaseDate = movie.getReleaseDate();
                if (releaseDate != null && releaseDate.contains("-")) {
                    year = releaseDate.split("-")[0];
                }

                // Safe genre names
                int[] genreIds = movie.getGenreIds();
                String[] genreNames;
                if (genreIds == null || genreIds.length == 0) {
                    genreNames = new String[]{"Unknown"};
                } else {
                    genreNames = Arrays.stream(genreIds)
                            .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                            .toArray(String[]::new);
                }

                // Connection if not first item
                if (i > start) {
                    Connection conn = record.getConnection();
                    if (conn != null) {
                        panel.addComponent(new Label("      |"));
                        panel.addComponent(new Label("Connection: " + conn.getConnectionValue()));
                        panel.addComponent(new Label("      |"));
                    }
                }

                panel.addComponent(new Label("▶ " + movie.getTitle() + " (" + year + "), Genres: " + String.join(", ", genreNames)));
            }
        }

        panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        panel.addComponent(new Button("OK", window::close));

        window.setComponent(panel);
        gui.addWindowAndWait(window);
    }
    
    public void startCountdownTimer(int seconds, Runnable onTimeout, Label timerLabel) {
        secondsRemaining = seconds;
        timerRunning = true;

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            if (timerRunning && secondsRemaining > 0) {
                secondsRemaining--;
                timerLabel.setText("Time left: " + secondsRemaining + "s");
            } else if (secondsRemaining == 0) {
                timerRunning = false;
                scheduler.shutdownNow();
                onTimeout.run(); // Timeout action
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    
    public String promptMoviePrefixWithTimeout(Runnable onTimeout) {
        BasicWindow window = new BasicWindow("Enter Movie Prefix");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        TextBox inputBox = new TextBox().setPreferredSize(new TerminalSize(40, 1));
        Button submitButton = new Button("Submit", window::close);
        Label timerLabel = new Label("Time left: 30s");

        panel.addComponent(new Label("Enter the prefix of the next movie:"));
        panel.addComponent(inputBox);
        panel.addComponent(timerLabel);
        panel.addComponent(submitButton);

        window.setComponent(panel);

        // Start countdown
        startCountdownTimer(30, () -> {
            window.close();
            onTimeout.run();
        }, timerLabel);

        gui.addWindowAndWait(window);

        timerRunning = false;
        if (scheduler != null) scheduler.shutdownNow();

        return inputBox.getText().trim();
    }
    
    public String promptMoviePrefixWithLiveSuggestions(MovieDataService movieDataService, Runnable onTimeout) {
        BasicWindow window = new BasicWindow("Enter Movie Prefix");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        TextBox inputBox = new TextBox().setPreferredSize(new TerminalSize(40, 1));
        Button submitButton = new Button("Submit", window::close);
        Label timerLabel = new Label("Time left: 30s");

        Panel suggestionPanel = new Panel(); // this will hold movie suggestions
        suggestionPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Start typing a movie title:"));
        panel.addComponent(inputBox);
        panel.addComponent(timerLabel);
        panel.addComponent(new Label("Suggestions:"));
        panel.addComponent(suggestionPanel);
        panel.addComponent(submitButton);

        window.setComponent(panel);

        // Live suggestion update
        inputBox.setTextChangeListener((newText, changedByUserInteraction) -> {
            suggestionPanel.removeAllComponents();
            if (!newText.trim().isEmpty()) {
                List<Movie> suggestions = movieDataService.searchMoviesByPrefix(newText.trim());
                for (int i = 0; i < Math.min(5, suggestions.size()); i++) {
                    suggestionPanel.addComponent(new Label((i + 1) + ". " + suggestions.get(i).getTitle()));
                }
            }
            try {
                gui.updateScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Start 30-second timer
        startCountdownTimer(30, () -> {
            window.close();
            onTimeout.run();
        }, timerLabel);

        gui.addWindowAndWait(window);

        timerRunning = false;
        if (scheduler != null) scheduler.shutdownNow();

        return inputBox.getText().trim();
    } 
    
    public String showGameTurn(
            
            int round,
            String playerName,
            List<HistoryRecord> history,
            Movie movie,
            WinCondition condition,
            MovieDataService movieDataService,
            Runnable onTimeout
    ) {
        BasicWindow window = new BasicWindow("Movie Game Turn");
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("=== Round " + round + " ==="));
        panel.addComponent(new Label("Current Player: " + playerName));
        panel.addComponent(new Label("── Recent History ──"));

        int start = Math.max(0, history.size() - 5);
        for (int i = start; i < history.size(); i++) {
            HistoryRecord record = history.get(i);
            Movie m = record.getMovie();

            String year = (m.getReleaseDate() != null && m.getReleaseDate().contains("-"))
                    ? m.getReleaseDate().split("-")[0] : "Unknown";

            int[] genreIds = m.getGenreIds();
            String[] genres = (genreIds == null || genreIds.length == 0)
                    ? new String[]{"Unknown"}
                    : Arrays.stream(genreIds)
                            .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                            .toArray(String[]::new);

            if (i > start && record.getConnection() != null) {
                panel.addComponent(new Label("  |"));
                panel.addComponent(new Label("Connection: " + record.getConnection().getConnectionValue()));
                panel.addComponent(new Label("  |"));
            }

            panel.addComponent(new Label("▶ " + m.getTitle() + " (" + year + "), Genres: " + String.join(", ", genres)));
        }

        panel.addComponent(new Label("── Current Movie Info ──"));
        String movieYear = (movie.getReleaseDate() != null && movie.getReleaseDate().contains("-"))
                ? movie.getReleaseDate().split("-")[0] : "Unknown";
        panel.addComponent(new Label(movie.getTitle() + " (" + movieYear + ")"));

        int[] currentGenres = movie.getGenreIds();
        String[] movieGenres = (currentGenres == null || currentGenres.length == 0)
                ? new String[]{"Unknown"}
                : Arrays.stream(currentGenres)
                        .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                        .toArray(String[]::new);
        panel.addComponent(new Label("Genres: " + String.join(", ", movieGenres)));

        MovieCredits credits = movieDataService.getMovieCredits(movie.getId());
        if (credits != null) {
            addCrew(panel, "Director", credits.getCrew(), "Director");
            addCrew(panel, "Writer", credits.getCrew(), "Writer", "Screenplay");
            addCrew(panel, "Cinematographer", credits.getCrew(), "Cinematographer", "Director of Photography");
            addCrew(panel, "Composer", credits.getCrew(), "Composer", "Original Music Composer");

            List<String> cast = credits.getCast() != null
                    ? credits.getCast().stream().limit(6).map(CastMember::getName).toList()
                    : List.of();

            if (!cast.isEmpty()) {
                panel.addComponent(new Label("Featuring: " + String.join(", ", cast)));
            }
        } else {
            panel.addComponent(new Label("(Credits not available)"));
        }

        panel.addComponent(new Label("Progress: " + condition.getCurrentCount() + " / " + condition.getTargetCount()));
        panel.addComponent(new Label("Enter the next movie prefix:"));

        TextBox inputBox = new TextBox().setPreferredSize(new TerminalSize(40, 1));
        panel.addComponent(inputBox);

        Label timerLabel = new Label("Time left: 30s");
        panel.addComponent(timerLabel);

        panel.addComponent(new Label("Suggestions:"));
        ComboBox<String> movieComboBox = new ComboBox<>();
        movieComboBox.setPreferredSize(new TerminalSize(40, 1));
        panel.addComponent(movieComboBox);

        Button submitButton = new Button("Submit", window::close);
        panel.addComponent(submitButton);

        inputBox.setTextChangeListener((newText, changedByUserInteraction) -> {
            movieComboBox.clearItems();
            if (!newText.trim().isEmpty()) {
                List<Movie> suggestions = movieDataService.searchMoviesByPrefix(newText.trim());
                for (Movie m : suggestions) {
                    movieComboBox.addItem(m.getTitle());
                }
            }
            try {
                gui.updateScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        startCountdownTimer(30, () -> {
            window.close();
            onTimeout.run();
        }, timerLabel);

        window.setComponent(panel);
        gui.addWindowAndWait(window);

        timerRunning = false;
        if (scheduler != null) scheduler.shutdownNow();

        String selectedTitle = movieComboBox.getSelectedItem();
        return selectedTitle != null ? selectedTitle.trim() : "";
    }

    private void addCrew(Panel panel, String label, List<CrewMember> crewList, String... jobs) {
        if (crewList == null) return;

        List<String> names = crewList.stream()
                .filter(c -> Arrays.asList(jobs).contains(c.getJob()))
                .map(CrewMember::getName)
                .toList();

        if (!names.isEmpty()) {
            panel.addComponent(new Label(label + ": " + String.join(", ", names)));
        }
    }    
    
    public void showErrorNonBlocking(String message) {
        new Thread(() -> {
            try {
                BasicWindow window = new BasicWindow("Error");
                Panel panel = new Panel();
                panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
                panel.addComponent(new Label("Error: " + message));
                panel.addComponent(new Button("OK", window::close));
                window.setComponent(panel);
                gui.addWindow(window);
            } catch (Exception ignored) {}
        }).start();
    }
}