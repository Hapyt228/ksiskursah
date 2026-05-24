package by.bsuir.filmcatalog.controller;

import by.bsuir.filmcatalog.dto.FilmDto;
import by.bsuir.filmcatalog.dto.tmdb.TmdbGenreDto;
import by.bsuir.filmcatalog.kp.KinopoiskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-контроллер для данных из Kinopoisk API.
 * URL остался /api/tmdb — фронтенд менять не нужно.
 */
@RestController
@RequestMapping("/api/tmdb")
@CrossOrigin(origins = "*")
public class TmdbController {

    private final KinopoiskService kpService;

    public TmdbController(KinopoiskService kpService) {
        this.kpService = kpService;
    }

    /** GET /api/tmdb/popular?page=1 */
    @GetMapping("/popular")
    public ResponseEntity<List<FilmDto>> getPopular(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(kpService.getPopular(page));
    }

    /** GET /api/tmdb/top_rated?page=1 */
    @GetMapping("/top_rated")
    public ResponseEntity<List<FilmDto>> getTopRated(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(kpService.getTopRated(page));
    }

    /** GET /api/tmdb/search?q=Inception&page=1 */
    @GetMapping("/search")
    public ResponseEntity<List<FilmDto>> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "1") int page) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(kpService.search(query, page));
    }

    /** GET /api/tmdb/search/director?q=Нолан */
    @GetMapping("/search/director")
    public ResponseEntity<List<FilmDto>> searchByDirector(
            @RequestParam("q") String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(kpService.searchByDirector(name));
    }

    /** GET /api/tmdb/movie/{kpId} */
    @GetMapping("/movie/{tmdbId}")
    public ResponseEntity<?> getMovie(@PathVariable Long tmdbId) {
        FilmDto dto = kpService.getDetails(tmdbId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /** GET /api/tmdb/movie/{kpId}/similar */
    @GetMapping("/movie/{tmdbId}/similar")
    public ResponseEntity<List<FilmDto>> getSimilar(@PathVariable Long tmdbId) {
        return ResponseEntity.ok(kpService.getSimilar(tmdbId));
    }

    /** GET /api/tmdb/genres */
    @GetMapping("/genres")
    public ResponseEntity<List<TmdbGenreDto>> getGenres() {
        return ResponseEntity.ok(kpService.getGenres());
    }

    /** GET /api/tmdb/discover — оставляем для совместимости, отдаёт популярные */
    @GetMapping("/discover")
    public ResponseEntity<List<FilmDto>> discover(
            @RequestParam(required = false) Integer genreId,
            @RequestParam(defaultValue = "1") int page) {
        if (genreId != null) {
            return ResponseEntity.ok(kpService.getByGenreId(genreId, 20));
        }
        return ResponseEntity.ok(kpService.getPopular(page));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleError(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Kinopoisk API ошибка: " + ex.getMessage()));
    }
}
