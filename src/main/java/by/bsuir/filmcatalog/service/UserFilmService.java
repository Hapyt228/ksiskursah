package by.bsuir.filmcatalog.service;

import by.bsuir.filmcatalog.dto.FilmDto;
import by.bsuir.filmcatalog.model.*;
import by.bsuir.filmcatalog.repository.*;
import by.bsuir.filmcatalog.kp.KinopoiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис взаимодействия пользователя с фильмами:
 *   - история просмотров (запись при открытии фильма)
 *   - статусы фильмов (смотрю, любимое и т.д.)
 *   - оценки фильмов (1-5 звёзд)
 *   - рекомендации с весовыми коэффициентами
 *
 * ─────────────────────────────────────────
 * АЛГОРИТМ РЕКОМЕНДАЦИЙ (приоритеты жанров)
 * ─────────────────────────────────────────
 * 1. Статус FAVOURITE / REWATCHING    → вес 5
 * 2. Статус WATCHING / PLANNED / COMPLETED → вес 3
 * 3. Статус POSTPONED                 → вес 1
 * 4. Только заход на страницу (view)  → вес 0.5
 * 5. Статус DROPPED                   → не учитывается
 * 6. Оценки: 5★=+5, 4★=+3, 3★=+1, <3 не учитываем
 *
 * Суммируем веса по жанрам → топ жанров → ищем фильмы этих жанров
 * → исключаем уже виденные → сортируем по совпадению жанра + рейтингу
 */
@Service
@Transactional
public class UserFilmService {

    @Autowired private UserRepository userRepository;
    @Autowired private FilmRepository filmRepository;
    @Autowired private MovieViewHistoryRepository viewHistoryRepository;
    @Autowired private MovieUserStatusRepository statusRepository;
    @Autowired private MovieRatingRepository ratingRepository;
    @Autowired private KinopoiskService tmdbService;

    // ========================
    // История просмотров
    // ========================

    /**
     * Вызывается при открытии страницы фильма.
     * Добавляет запись в историю (не чаще раза в 30 минут для одного фильма).
     *
     * Куда вставить: FilmController.getFilmById() — после получения фильма.
     */
    public void recordView(String username, Long filmId) {
        userRepository.findByUsername(username).ifPresent(user -> {
            filmRepository.findById(filmId).ifPresent(film -> {
                // Дедупликация: не записываем, если был просмотр < 30 мин назад
                boolean recentExists = viewHistoryRepository.existsByUserAndFilmAndViewedAtAfter(
                    user, film, LocalDateTime.now().minusMinutes(30)
                );
                if (!recentExists) {
                    viewHistoryRepository.save(new MovieViewHistory(user, film));
                }
            });
        });
    }

    /**
     * История просмотров пользователя (последние N).
     */
    @Transactional(readOnly = true)
    public List<FilmDto> getViewHistory(String username) {
        User user = findUserOrThrow(username);
        // Один фильм может появляться несколько раз — берём только последний запись для каждого
        Set<Long> seen = new LinkedHashSet<>();
        return viewHistoryRepository.findByUserOrderByViewedAtDesc(user)
                .stream()
                .filter(h -> seen.add(h.getFilm().getId())) // дедупликация: первый встреченный = последний по времени
                .limit(50)
                .map(h -> {
                    FilmDto dto = toDto(h.getFilm());
                    dto.setViewedAt(h.getViewedAt()); // передаём дату просмотра
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ========================
    // Статусы фильмов
    // ========================

    /**
     * Устанавливает или обновляет статус фильма у пользователя.
     */
    public void setStatus(String username, Long filmId, MovieUserStatus.WatchStatus status) {
        User user = findUserOrThrow(username);
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new NoSuchElementException("Фильм не найден"));

        MovieUserStatus existing = statusRepository.findByUserAndFilm(user, film)
                .orElse(new MovieUserStatus(user, film, status));
        existing.setStatus(status);
        statusRepository.save(existing);
    }

    /**
     * Возвращает текущий статус фильма для пользователя (или null).
     */
    @Transactional(readOnly = true)
    public Optional<String> getStatus(String username, Long filmId) {
        return userRepository.findByUsername(username).flatMap(user ->
            filmRepository.findById(filmId).flatMap(film ->
                statusRepository.findByUserAndFilm(user, film)
                    .map(s -> s.getStatus().name())
            )
        );
    }

    /**
     * Удалить статус (убрать из списков).
     */
    public void removeStatus(String username, Long filmId) {
        userRepository.findByUsername(username).ifPresent(user ->
            filmRepository.findById(filmId).ifPresent(film ->
                statusRepository.findByUserAndFilm(user, film)
                    .ifPresent(statusRepository::delete)
            )
        );
    }

    // ========================
    // Оценки фильмов
    // ========================

    /**
     * Поставить или обновить оценку фильма (1-5 звёзд).
     */
    public void rateFilm(String username, Long filmId, Integer stars) {
        if (stars < 1 || stars > 10) throw new IllegalArgumentException("Оценка от 1 до 10");

        User user = findUserOrThrow(username);
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new NoSuchElementException("Фильм не найден"));

        MovieRating rating = ratingRepository.findByUserAndFilm(user, film)
                .orElse(new MovieRating(user, film, stars));
        rating.setStars(stars);
        ratingRepository.save(rating);
    }

    /**
     * Оценка текущего пользователя для фильма.
     */
    @Transactional(readOnly = true)
    public Optional<Integer> getUserRating(String username, Long filmId) {
        return userRepository.findByUsername(username).flatMap(user ->
            filmRepository.findById(filmId).flatMap(film ->
                ratingRepository.findByUserAndFilm(user, film)
                    .map(MovieRating::getStars)
            )
        );
    }

    // ========================
    // РЕКОМЕНДАЦИИ (главная)
    // ========================

    /**
     * Возвращает список рекомендуемых фильмов для авторизованного пользователя.
     *
     * Алгоритм:
     *  1. Собираем веса жанров из статусов (с CASE WHEN)
     *  2. Добавляем веса из оценок пользователя
     *  3. Добавляем веса из истории просмотров (каждый просмотр = +0.5)
     *  4. Нормализуем и берём топ-3 жанра
     *  5. Находим фильмы этих жанров, исключаем уже виденные
     *  6. Сортируем: совпадение жанра (приоритет) + глобальный рейтинг
     *  7. Если данных нет — возвращаем топ по глобальному рейтингу
     */
    @Transactional(readOnly = true)
    public List<FilmDto> getRecommendations(String username, int limit) {
        User user = findUserOrThrow(username);

        // Шаг 1: веса жанров — собираем genre строки (они же хранят имя жанра из TMDb)
        Map<String, Double> genreWeights = new LinkedHashMap<>();

        statusRepository.findWeightedGenresByUser(user).forEach(row -> {
            String genre = (String) row[0];
            double weight = ((Number) row[1]).doubleValue();
            if (genre != null) genreWeights.merge(genre, weight, Double::sum);
        });

        ratingRepository.findWeightedGenresByRating(user).forEach(row -> {
            String genre = (String) row[0];
            double weight = ((Number) row[1]).doubleValue();
            if (genre != null) genreWeights.merge(genre, weight, Double::sum);
        });

        viewHistoryRepository.findByUserOrderByViewedAtDesc(user).stream()
            .limit(100)
            .forEach(h -> {
                if (h.getFilm().getGenre() != null)
                    genreWeights.merge(h.getFilm().getGenre(), 0.5, Double::sum);
            });

        // Шаг 2: tmdbId уже посмотренных (чтобы не повторяться)
        Set<Long> seenTmdbIds = new HashSet<>();
        viewHistoryRepository.findViewedFilmIdsByUser(user).forEach(localId ->
            filmRepository.findById(localId).ifPresent(f -> {
                if (f.getTmdbId() != null) seenTmdbIds.add(f.getTmdbId());
            })
        );
        statusRepository.findFilmIdsByUser(user).forEach(localId ->
            filmRepository.findById(localId).ifPresent(f -> {
                if (f.getTmdbId() != null) seenTmdbIds.add(f.getTmdbId());
            })
        );

        // Шаг 3: если истории нет — популярные из TMDb
        if (genreWeights.isEmpty()) {
            return tmdbService.getRecommendationsByGenreIds(
                    Collections.emptyList(), seenTmdbIds, limit);
        }

        // Шаг 4: топ-3 жанра по весу → переводим в TMDb genre_id
        List<String> topGenreNames = genreWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Находим TMDb genre_id по имени жанра
        List<Integer> topGenreIds = topGenreNames.stream()
                .map(name -> tmdbService.resolveGenreIdByName(name))
                .filter(id -> id != null)
                .collect(Collectors.toList());

        // Шаг 5: запрашиваем TMDb Discover
        return tmdbService.getRecommendationsByGenreIds(topGenreIds, seenTmdbIds, limit);
    }

    // ========================
    // Методы для TMDb-фильмов
    // ========================

    /**
     * Записывает просмотр TMDb-фильма.
     * Автоматически сохраняет фильм в локальную БД (если ещё нет).
     * @param filmData  Данные из фронтенда (может быть null — тогда придётся идти за деталями в TMDb)
     * @return localId — id фильма в локальной БД
     */
    public Long recordTmdbView(String username, Long tmdbId, FilmDto filmData) {
        Long localId = resolveOrCreateLocalFilm(tmdbId, filmData);
        recordView(username, localId);
        return localId;
    }

    /**
     * Возвращает локальный id фильма, создавая его если нужно.
     * Работает по схеме:
     *   1. Есть в локальной БД фильм с этим tmdbId? → возвращаем его id.
     *   2. Есть filmData из фронтенда? → сохраняем эти данные как новый Film.
     *   3. Иначе запрашиваем детали от TMDb и сохраняем.
     */
    @Transactional
    public Long resolveOrCreateLocalFilm(Long tmdbId, FilmDto filmData) {
        // 1. Ищем уже существующую локальную копию
        Optional<Film> existing = filmRepository.findByTmdbId(tmdbId);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        // 2. Есть данные с фронтенда — используем их
        FilmDto source = filmData;

        // 3. Иначе — запрашиваем TMDb
        if (source == null || source.getTitle() == null || source.getTitle().isBlank()) {
            source = tmdbService.getDetails(tmdbId);
        }
        if (source == null) {
            throw new NoSuchElementException("Не удалось получить данные TMDb-фильма: " + tmdbId);
        }

        // 4. Сохраняем новый Film
        Film film = new Film();
        film.setTmdbId(tmdbId);
        film.setTitle(source.getTitle() != null ? source.getTitle() : "Неизвестно");
        film.setOriginalTitle(source.getOriginalTitle());
        film.setYear(source.getYear() != null ? source.getYear() : 0);
        film.setGenre(source.getGenre() != null ? source.getGenre() : "Прочее");
        film.setDirector(source.getDirector() != null ? source.getDirector() : "Неизвестно");
        film.setCountry(source.getCountry());
        film.setDescription(source.getDescription());
        film.setRating(source.getRating());
        film.setDurationMinutes(source.getDurationMinutes());
        film.setPosterUrl(source.getPosterUrl());
        film.setLanguage(source.getLanguage());
        film.setTags(source.getTags());

        return filmRepository.save(film).getId();
    }

    /**
     * Получить статус фильма по tmdbId.
     */
    @Transactional(readOnly = true)
    public Optional<String> getStatusByTmdbId(String username, Long tmdbId) {
        return filmRepository.findByTmdbId(tmdbId).flatMap(film ->
            userRepository.findByUsername(username).flatMap(user ->
                statusRepository.findByUserAndFilm(user, film)
                    .map(s -> s.getStatus().name())
            )
        );
    }

    /**
     * Удалить статус фильма по tmdbId.
     */
    public void removeStatusByTmdbId(String username, Long tmdbId) {
        filmRepository.findByTmdbId(tmdbId).ifPresent(film ->
            userRepository.findByUsername(username).ifPresent(user ->
                statusRepository.findByUserAndFilm(user, film)
                    .ifPresent(statusRepository::delete)
            )
        );
    }

    /**
     * Получить оценку пользователя для TMDb-фильма.
     */
    @Transactional(readOnly = true)
    public Optional<Integer> getRatingByTmdbId(String username, Long tmdbId) {
        return filmRepository.findByTmdbId(tmdbId).flatMap(film ->
            userRepository.findByUsername(username).flatMap(user ->
                ratingRepository.findByUserAndFilm(user, film)
                    .map(MovieRating::getStars)
            )
        );
    }

    // ========================
    // Вспомогательные
    // ========================

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + username));
    }

    private FilmDto toDto(Film film) {
        FilmDto dto = new FilmDto();
        dto.setId(film.getId());
        dto.setTmdbId(film.getTmdbId());
        dto.setTitle(film.getTitle());
        dto.setOriginalTitle(film.getOriginalTitle());
        dto.setYear(film.getYear());
        dto.setGenre(film.getGenre());
        dto.setDirector(film.getDirector());
        dto.setCountry(film.getCountry());
        dto.setDescription(film.getDescription());
        dto.setRating(film.getRating());
        dto.setDurationMinutes(film.getDurationMinutes());
        dto.setPosterUrl(film.getPosterUrl());
        dto.setLanguage(film.getLanguage());
        dto.setTags(film.getTags());
        return dto;
    }
}
