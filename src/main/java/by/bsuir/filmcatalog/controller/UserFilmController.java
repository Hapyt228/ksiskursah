package by.bsuir.filmcatalog.controller;

import by.bsuir.filmcatalog.dto.FilmDto;
import by.bsuir.filmcatalog.model.MovieUserStatus;
import by.bsuir.filmcatalog.service.UserFilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

import java.util.List;
import java.util.Map;

/**
 * REST-контроллер для пользовательского взаимодействия с фильмами.
 *
 * POST   /api/user/films/{id}/view          — записать просмотр
 * PUT    /api/user/films/{id}/status        — поставить статус
 * DELETE /api/user/films/{id}/status        — снять статус
 * GET    /api/user/films/{id}/status        — получить статус
 * PUT    /api/user/films/{id}/rating        — поставить оценку (1-5★)
 * GET    /api/user/films/{id}/rating        — моя оценка
 * GET    /api/user/recommendations          — рекомендации для главной страницы
 * GET    /api/user/history                  — история просмотров
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserFilmController {

    @Autowired
    private UserFilmService userFilmService;

    // ========================
    // История просмотров
    // ========================

    /**
     * Записывает просмотр локального фильма по локальному id.
     * Вызывается фронтендом при открытии страницы детального просмотра (local film).
     *
     * Response: { "recorded": true }
     */
    @PostMapping("/films/{id}/view")
    public ResponseEntity<?> recordView(@PathVariable Long id, Authentication auth) {
        userFilmService.recordView(auth.getName(), id);
        return ResponseEntity.ok(Map.of("recorded", true));
    }

    /**
     * Записывает просмотр TMDb-фильма по tmdbId.
     * Фильм автоматически сохраняется в локальную БД (если ещё не сохранён).
     *
     * POST /api/user/tmdb/{tmdbId}/view
     * Body (optional): { "title": "...", "genre": "...", ... }  — данные для сохранения
     * Response: { "recorded": true, "localId": 42 }
     */
    @PostMapping("/tmdb/{tmdbId}/view")
    public ResponseEntity<?> recordTmdbView(
            @PathVariable Long tmdbId,
            @RequestBody(required = false) FilmDto filmData,
            Authentication auth) {
        try {
            Long localId = userFilmService.recordTmdbView(auth.getName(), tmdbId, filmData);
            return ResponseEntity.ok(Map.of("recorded", true, "localId", localId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Установить статус TMDb-фильма по tmdbId.
     * Фильм автоматически сохраняется в локальную БД (если ещё не сохранён).
     *
     * PUT /api/user/tmdb/{tmdbId}/status
     * Body: { "status": "FAVOURITE", "filmData": { "title": "...", ... } }
     */
    @PutMapping("/tmdb/{tmdbId}/status")
    public ResponseEntity<?> setTmdbStatus(
            @PathVariable Long tmdbId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        String statusStr = (String) body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Поле status обязательно"));
        }
        try {
            MovieUserStatus.WatchStatus status = MovieUserStatus.WatchStatus.valueOf(statusStr.toUpperCase());
            Long localId = userFilmService.resolveOrCreateLocalFilm(tmdbId, extractFilmDto(body));
            userFilmService.setStatus(auth.getName(), localId, status);
            return ResponseEntity.ok(Map.of("status", status.name(), "localId", localId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Недопустимый статус: " + statusStr));
        }
    }

    /**
     * Получить статус TMDb-фильма.
     * GET /api/user/tmdb/{tmdbId}/status
     */
    @GetMapping("/tmdb/{tmdbId}/status")
    public ResponseEntity<?> getTmdbStatus(@PathVariable Long tmdbId, Authentication auth) {
        String status = userFilmService.getStatusByTmdbId(auth.getName(), tmdbId).orElse("");
        return ResponseEntity.ok(Map.of("status", status));
    }

    /**
     * Убрать статус TMDb-фильма.
     * DELETE /api/user/tmdb/{tmdbId}/status
     */
    @DeleteMapping("/tmdb/{tmdbId}/status")
    public ResponseEntity<?> removeTmdbStatus(@PathVariable Long tmdbId, Authentication auth) {
        userFilmService.removeStatusByTmdbId(auth.getName(), tmdbId);
        return ResponseEntity.ok(Map.of("removed", true));
    }

    /**
     * Поставить оценку TMDb-фильму.
     * PUT /api/user/tmdb/{tmdbId}/rating
     * Body: { "stars": 5, "filmData": { "title": "...", ... } }
     */
    @PutMapping("/tmdb/{tmdbId}/rating")
    public ResponseEntity<?> rateTmdbFilm(
            @PathVariable Long tmdbId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Integer stars = (Integer) body.get("stars");
        if (stars == null || stars < 1 || stars > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Оценка должна быть от 1 до 5"));
        }
        Long localId = userFilmService.resolveOrCreateLocalFilm(tmdbId, extractFilmDto(body));
        userFilmService.rateFilm(auth.getName(), localId, stars);
        return ResponseEntity.ok(Map.of("stars", stars, "localId", localId));
    }

    /**
     * Получить оценку TMDb-фильма.
     * GET /api/user/tmdb/{tmdbId}/rating
     */
    @GetMapping("/tmdb/{tmdbId}/rating")
    public ResponseEntity<?> getTmdbRating(@PathVariable Long tmdbId, Authentication auth) {
        Integer stars = userFilmService.getRatingByTmdbId(auth.getName(), tmdbId).orElse(0);
        return ResponseEntity.ok(Map.of("stars", stars));
    }

    // Вспомогательный: извлекает filmData из тела запроса (для автосохранения фильма)
    @SuppressWarnings("unchecked")
    private FilmDto extractFilmDto(Map<String, Object> body) {
        Object filmDataRaw = body.get("filmData");
        if (filmDataRaw instanceof Map) {
            Map<String, Object> fd = (Map<String, Object>) filmDataRaw;
            FilmDto dto = new FilmDto();
            dto.setTitle((String) fd.getOrDefault("title", ""));
            dto.setOriginalTitle((String) fd.get("originalTitle"));
            dto.setGenre((String) fd.getOrDefault("genre", "Прочее"));
            dto.setDirector((String) fd.getOrDefault("director", "Неизвестно"));
            dto.setDescription((String) fd.get("description"));
            dto.setPosterUrl((String) fd.get("posterUrl"));
            dto.setLanguage((String) fd.get("language"));
            dto.setCountry((String) fd.get("country"));
            dto.setTags((String) fd.get("tags"));
            if (fd.get("year") instanceof Number) dto.setYear(((Number) fd.get("year")).intValue());
            if (fd.get("rating") instanceof Number) dto.setRating(((Number) fd.get("rating")).doubleValue());
            if (fd.get("durationMinutes") instanceof Number) dto.setDurationMinutes(((Number) fd.get("durationMinutes")).intValue());
            if (fd.get("tmdbId") instanceof Number) dto.setTmdbId(((Number) fd.get("tmdbId")).longValue());
            return dto;
        }
        return null;
    }

    /**
     * История просмотров пользователя.
     *
     * Response: [ { "id": 1, "title": "...", ... }, ... ]
     */
    @GetMapping("/history")
    public ResponseEntity<List<FilmDto>> getHistory(Authentication auth) {
        return ResponseEntity.ok(userFilmService.getViewHistory(auth.getName()));
    }

    // ========================
    // Статусы фильмов
    // ========================

    /**
     * Устанавливает статус фильма.
     *
     * Request:  { "status": "FAVOURITE" }
     * Response: { "status": "FAVOURITE" }
     *
     * Допустимые значения status:
     *   WATCHING, PLANNED, DROPPED, COMPLETED, FAVOURITE, REWATCHING, POSTPONED
     */
    @PutMapping("/films/{id}/status")
    public ResponseEntity<?> setStatus(@PathVariable Long id,
                                       @RequestBody Map<String, String> body,
                                       Authentication auth) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Поле status обязательно"));
        }
        try {
            MovieUserStatus.WatchStatus status = MovieUserStatus.WatchStatus.valueOf(statusStr.toUpperCase());
            userFilmService.setStatus(auth.getName(), id, status);
            return ResponseEntity.ok(Map.of("status", status.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Недопустимый статус: " + statusStr));
        }
    }

    /**
     * Получить текущий статус фильма у пользователя.
     *
     * Response: { "status": "FAVOURITE" } или { "status": null }
     */
    @GetMapping("/films/{id}/status")
    public ResponseEntity<?> getStatus(@PathVariable Long id, Authentication auth) {
        String status = userFilmService.getStatus(auth.getName(), id).orElse(null);
        return ResponseEntity.ok(Map.of("status", status != null ? status : ""));
    }

    /**
     * Убрать статус фильма.
     */
    @DeleteMapping("/films/{id}/status")
    public ResponseEntity<?> removeStatus(@PathVariable Long id, Authentication auth) {
        userFilmService.removeStatus(auth.getName(), id);
        return ResponseEntity.ok(Map.of("removed", true));
    }

    // ========================
    // Оценки фильмов
    // ========================

    /**
     * Поставить или обновить оценку фильму (1-5 звёзд).
     *
     * Request:  { "stars": 5 }
     * Response: { "stars": 5 }
     */
    @PutMapping("/films/{id}/rating")
    public ResponseEntity<?> rateFilm(@PathVariable Long id,
                                      @RequestBody Map<String, Integer> body,
                                      Authentication auth) {
        Integer stars = body.get("stars");
        if (stars == null || stars < 1 || stars > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Оценка должна быть от 1 до 5"));
        }
        userFilmService.rateFilm(auth.getName(), id, stars);
        return ResponseEntity.ok(Map.of("stars", stars));
    }

    /**
     * Получить свою оценку фильма.
     *
     * Response: { "stars": 4 } или { "stars": null }
     */
    @GetMapping("/films/{id}/rating")
    public ResponseEntity<?> getUserRating(@PathVariable Long id, Authentication auth) {
        Integer stars = userFilmService.getUserRating(auth.getName(), id).orElse(null);
        return ResponseEntity.ok(Map.of("stars", stars != null ? stars : 0));
    }

    // ========================
    // Рекомендации
    // ========================

    /**
     * Рекомендации для главной страницы.
     * Алгоритм с весовыми коэффициентами (см. UserFilmService).
     *
     * GET /api/user/recommendations?limit=12
     *
     * Response:
     * [
     *   {
     *     "id": 5, "title": "Интерстеллар", "genre": "Фантастика",
     *     "rating": 8.6, "year": 2014, ...
     *   },
     *   ...
     * ]
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<FilmDto>> getRecommendations(
            @RequestParam(defaultValue = "12") int limit,
            Authentication auth) {
        List<FilmDto> recommendations = userFilmService.getRecommendations(auth.getName(), limit);
        return ResponseEntity.ok(recommendations);
    }
}
