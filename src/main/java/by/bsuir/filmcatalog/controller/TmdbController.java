package by.bsuir.filmcatalog.controller;

import by.bsuir.filmcatalog.dto.FilmDto;
import by.bsuir.filmcatalog.dto.tmdb.TmdbGenreDto;
import by.bsuir.filmcatalog.tmdb.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-контроллер для данных из TMDb API.
 * Базовый URL: /api/tmdb
 *
 * Эндпоинты:
 *   GET /api/tmdb/popular          — популярные фильмы (страница 1..N)
 *   GET /api/tmdb/search?q=...     — поиск по названию
 *   GET /api/tmdb/movie/{tmdbId}   — детали фильма
 *   GET /api/tmdb/genres           — список жанров
 *   GET /api/tmdb/movie/{tmdbId}/similar — похожие фильмы
 *
 * Всё это публичные эндпоинты (не требуют авторизации).
 * Авторизованные пользовательские действия (статус, рейтинг, просмотр) —
 * по-прежнему в UserFilmController под /api/user/films/{id}/...
 */
@RestController
@RequestMapping("/api/tmdb")
@CrossOrigin(origins = "*")
public class TmdbController {

    private final TmdbService tmdbService;

    public TmdbController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    // ========================
    // Популярные фильмы
    // ========================

    /**
     * GET /api/tmdb/popular?page=1
     * Возвращает список популярных фильмов (до 20 за запрос).
     */
    @GetMapping("/popular")
    public ResponseEntity<List<FilmDto>> getPopular(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getPopular(page));
    }

    // ========================
    // Поиск
    // ========================

    /**
     * GET /api/tmdb/search?q=Inception&page=1
     * Поиск фильмов по названию через TMDb.
     */
    @GetMapping("/search")
    public ResponseEntity<List<FilmDto>> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "1") int page) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tmdbService.search(query, page));
    }

    /**
     * GET /api/tmdb/search/director?q=Nolan
     * Поиск фильмов по имени режиссёра.
     * Алгоритм: /search/person → person_id → /discover/movie?with_crew=id
     */
    @GetMapping("/search/director")
    public ResponseEntity<List<FilmDto>> searchByDirector(
            @RequestParam("q") String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tmdbService.searchByDirector(name));
    }

    // ========================
    // Детали фильма
    // ========================

    /**
     * GET /api/tmdb/movie/{tmdbId}
     * Детальная информация о фильме: жанры, режиссёр, страна, credits.
     */
    @GetMapping("/movie/{tmdbId}")
    public ResponseEntity<?> getMovie(@PathVariable Long tmdbId) {
        FilmDto dto = tmdbService.getDetails(tmdbId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/tmdb/movie/{tmdbId}/similar
     * Похожие фильмы для блока "Рекомендации" на странице фильма.
     */
    @GetMapping("/movie/{tmdbId}/similar")
    public ResponseEntity<List<FilmDto>> getSimilar(@PathVariable Long tmdbId) {
        return ResponseEntity.ok(tmdbService.getSimilar(tmdbId));
    }

    // ========================
    // Жанры
    // ========================

    /**
     * GET /api/tmdb/genres
     * Полный список жанров TMDb (id + name на русском).
     * Используется для фильтра на фронтенде.
     */
    @GetMapping("/genres")
    public ResponseEntity<List<TmdbGenreDto>> getGenres() {
        return ResponseEntity.ok(tmdbService.getGenres());
    }

    // ========================
    // Топ фильмов по рейтингу
    // ========================

    /**
     * GET /api/tmdb/top_rated?page=1
     * Возвращает список фильмов с наивысшим рейтингом по версии TMDb.
     */
    @GetMapping("/top_rated")
    public ResponseEntity<List<FilmDto>> getTopRated(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.getTopRated(page));
    }

    /**
     * GET /api/tmdb/discover?genreId=28&yearFrom=2000&yearTo=2023&rating=7.5&page=1
     * Универсальный Discover — все параметры опциональны.
     * Если genreId не указан, фильтрует по году/рейтингу без ограничения жанра.
     */
    @GetMapping("/discover")
    public ResponseEntity<List<FilmDto>> discover(
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Double rating,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbService.discover(genreId, yearFrom, yearTo, rating, page));
    }

    // ========================
    // Обработка ошибок
    // ========================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleError(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "TMDb ошибка: " + ex.getMessage()));
    }
}
