package by.bsuir.filmcatalog.controller;

import by.bsuir.filmcatalog.dto.FilmDto;
import by.bsuir.filmcatalog.service.FilmService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST-контроллер веб-службы «Каталог фильмов».
 * Базовый URL: /api/films
 *
 * Архитектура: клиент-сервер, HTTP/JSON, RESTful.
 */
@RestController
@RequestMapping("/api/films")
@CrossOrigin(origins = "*")   // Разрешаем CORS для фронтенда
public class FilmController {

    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    // ========================
    // CRUD эндпоинты
    // ========================

    /**
     * GET /api/films — получить все фильмы.
     */
    @GetMapping
    public ResponseEntity<List<FilmDto>> getAllFilms() {
        return ResponseEntity.ok(filmService.getAllFilms());
    }

    /**
     * GET /api/films/{id} — получить фильм по ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FilmDto> getFilmById(@PathVariable Long id) {
        return ResponseEntity.ok(filmService.getFilmById(id));
    }

    /**
     * POST /api/films — создать новый фильм.
     */
    @PostMapping
    public ResponseEntity<FilmDto> createFilm(@Valid @RequestBody FilmDto dto) {
        FilmDto created = filmService.createFilm(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/films/{id} — обновить фильм.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FilmDto> updateFilm(@PathVariable Long id,
                                               @Valid @RequestBody FilmDto dto) {
        return ResponseEntity.ok(filmService.updateFilm(id, dto));
    }

    /**
     * DELETE /api/films/{id} — удалить фильм.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFilm(@PathVariable Long id) {
        filmService.deleteFilm(id);
        return ResponseEntity.ok(Map.of("message", "Фильм успешно удалён"));
    }

    // ========================
    // Поиск и фильтрация
    // ========================

    /**
     * GET /api/films/search?q=запрос — полнотекстовый поиск.
     */
    @GetMapping("/search")
    public ResponseEntity<List<FilmDto>> searchFilms(@RequestParam("q") String query) {
        return ResponseEntity.ok(filmService.searchFilms(query));
    }

    /**
     * GET /api/films/filter — фильтрация по нескольким параметрам.
     * Параметры (все необязательные): genre, yearFrom, yearTo, minRating, country
     */
    @GetMapping("/filter")
    public ResponseEntity<List<FilmDto>> filterFilms(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) String country) {
        return ResponseEntity.ok(filmService.filterFilms(genre, yearFrom, yearTo, minRating, country));
    }

    /**
     * GET /api/films/top?limit=10 — топ фильмов по рейтингу.
     */
    @GetMapping("/top")
    public ResponseEntity<List<FilmDto>> getTopRated(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(filmService.getTopRated(limit));
    }

    // ========================
    // Веб-служба рекомендаций
    // ========================

    /**
     * GET /api/films/{id}/recommendations — рекомендации для фильма.
     * Возвращает похожие фильмы по жанру и режиссёру.
     */
    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<FilmDto>> getRecommendations(@PathVariable Long id) {
        return ResponseEntity.ok(filmService.getRecommendations(id));
    }

    // ========================
    // Метаданные
    // ========================

    /**
     * GET /api/films/meta/genres — список всех жанров.
     */
    @GetMapping("/meta/genres")
    public ResponseEntity<List<String>> getAllGenres() {
        return ResponseEntity.ok(filmService.getAllGenres());
    }

    /**
     * GET /api/films/meta/countries — список всех стран.
     */
    @GetMapping("/meta/countries")
    public ResponseEntity<List<String>> getAllCountries() {
        return ResponseEntity.ok(filmService.getAllCountries());
    }

    // ========================
    // Обработка ошибок
    // ========================

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + ex.getMessage()));
    }
}
