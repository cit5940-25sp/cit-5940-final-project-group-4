import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import service.tmdbApi.TMDBMovieService;
import controller.GameController;

import java.util.List;

@Slf4j
public class Main {
    public static void main(String[] args) {
        
        GameController controller = new GameController();
        controller.startGame();
        
        
//        log.info("Start getting TMDB popular movies...");
//        List<Movie> movies = TMDBMovieService.getTop5000PopularMovies();
//
//        if (movies != null) {
//            log.info("Get success, total {} movies", movies.size());
//            System.out.println(movies);
//        } else {
//            log.error("Failed to obtain movie data!");
//        }
    }
}
