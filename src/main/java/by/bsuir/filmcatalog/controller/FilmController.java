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

// REST контроллер для работы с фильмами из локальной базы данных
@RestController
@RequestMapping("/api/films")
@CrossOrigin(origins = "*")
public class FilmController {

    @Autowired
    private FilmService filmService;

    // Получить все фильмы
    @GetMapping
    public ResponseEntity<List<FilmDto>> getAllFilms() {
        return ResponseEntity.ok(filmService.getAllFilms());
    }

    // Получить фильм по id
    @GetMapping("/{id}")
    public ResponseEntity<FilmDto> getFilmById(@PathVariable Long id) {
        return ResponseEntity.ok(filmService.getFilmById(id));
    }

    // Добавить фильм
    @PostMapping
    public ResponseEntity<FilmDto> createFilm(@Valid @RequestBody FilmDto dto) {
        FilmDto created = filmService.createFilm(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Обновить фильм
    @PutMapping("/{id}")
    public ResponseEntity<FilmDto> updateFilm(@PathVariable Long id,
                                               @Valid @RequestBody FilmDto dto) {
        return ResponseEntity.ok(filmService.updateFilm(id, dto));
    }

    // Удалить фильм
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFilm(@PathVariable Long id) {
        filmService.deleteFilm(id);
        return ResponseEntity.ok(Map.of("message", "Фильм успешно удалён"));
    }

    // Поиск по названию
    @GetMapping("/search")
    public ResponseEntity<List<FilmDto>> searchFilms(@RequestParam("q") String query) {
        return ResponseEntity.ok(filmService.searchFilms(query));
    }

    // Топ фильмов по рейтингу
    @GetMapping("/top")
    public ResponseEntity<List<FilmDto>> getTopRated(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(filmService.getTopRated(limit));
    }

    // Похожие фильмы для страницы фильма
    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<FilmDto>> getRecommendations(@PathVariable Long id) {
        return ResponseEntity.ok(filmService.getRecommendations(id));
    }

    // Список всех жанров
    @GetMapping("/meta/genres")
    public ResponseEntity<List<String>> getAllGenres() {
        return ResponseEntity.ok(filmService.getAllGenres());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка: " + ex.getMessage()));
    }
}
