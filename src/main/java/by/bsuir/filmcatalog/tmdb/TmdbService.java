package by.bsuir.filmcatalog.tmdb;

import by.bsuir.filmcatalog.dto.FilmDto;
import by.bsuir.filmcatalog.dto.tmdb.TmdbGenreDto;
import by.bsuir.filmcatalog.dto.tmdb.TmdbMovieDto;
import by.bsuir.filmcatalog.dto.tmdb.TmdbPageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис для работы с TMDb API.
 *
 * Ответственность:
 *  - конвертирует TmdbMovieDto → FilmDto (наш общий формат)
 *  - кэширует список жанров (один запрос на старте вместо N запросов)
 *  - предоставляет методы популярного, поиска, деталей и рекомендаций
 *
 * Идентификатор фильма в TMDb хранится в FilmDto.id как отрицательный (tmdbId * -1),
 * чтобы фронтенд мог отличить TMDb-фильм от локального.
 *
 * ВАЖНО: tmdbId хранится в FilmDto через поле id со знаком «-» НЕ используется.
 * Вместо этого в FilmDto есть отдельное поле tmdbId (Long).
 */
@Service
public class TmdbService {

    private final TmdbClient client;

    @Value("${tmdb.api.image-base-url:https://image.tmdb.org/t/p/w500}")
    private String imageBaseUrl;

    /** Кэш жанров: tmdbGenreId → name (ru-RU). Заполняется лениво. */
    private final Map<Integer, String> genreCache = new ConcurrentHashMap<>();

    public TmdbService(TmdbClient client) {
        this.client = client;
    }

    // ========================
    // Публичные методы
    // ========================

    /**
     * Список популярных фильмов (страница 1, до 20 фильмов).
     */
    public List<FilmDto> getPopular(int page) {
        TmdbPageResponse response = client.getPopularMovies(page);
        return convertList(response);
    }

    /**
     * Поиск фильмов по названию.
     */
    public List<FilmDto> search(String query, int page) {
        TmdbPageResponse response = client.searchMovies(query, page);
        return convertList(response);
    }

    /**
     * Детали конкретного фильма по его TMDb ID.
     * Делает один запрос с append_to_response=credits (режиссёр внутри).
     */
    public FilmDto getDetails(Long tmdbId) {
        TmdbMovieDto movie = client.getMovieDetails(tmdbId);
        if (movie == null) return null;
        return toFilmDto(movie);
    }

    /**
     * Список всех жанров TMDb (для фильтра на фронтенде).
     * Результат берётся из кэша после первого запроса.
     */
    public List<TmdbGenreDto> getGenres() {
        if (genreCache.isEmpty()) {
            warmupGenreCache();
        }
        return genreCache.entrySet().stream()
                .map(e -> {
                    TmdbGenreDto g = new TmdbGenreDto();
                    g.setId(e.getKey());
                    g.setName(e.getValue());
                    return g;
                })
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Похожие фильмы для страницы фильма (блок "Похожие").
     */
    public List<FilmDto> getSimilar(Long tmdbId) {
        TmdbPageResponse response = client.getMovieRecommendations(tmdbId, 1);
        return convertList(response);
    }

    /**
     * Фильмы определённого жанра (для рекомендательного алгоритма).
     * @param tmdbGenreId  ID жанра в TMDb
     * @param limit        максимальное количество фильмов
     */
    public List<FilmDto> getByGenreId(Integer tmdbGenreId, int limit) {
        TmdbPageResponse response = client.discoverByGenre(tmdbGenreId, 1);
        return convertList(response).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Универсальный Discover с опциональными фильтрами (для /api/tmdb/discover).
     */
    public List<FilmDto> discover(Integer genreId, Integer yearFrom, Integer yearTo,
                                  Double voteAverageGte, int page) {
        TmdbPageResponse response = client.discover(genreId, yearFrom, yearTo, voteAverageGte, page);
        return convertList(response);
    }

    /**
     * Устаревший метод — оставлен для совместимости.
     */
    public List<FilmDto> discoverByGenre(Integer tmdbGenreId, int page) {
        return discover(tmdbGenreId, null, null, null, page);
    }

    /**
     * Рекомендации на основе списка tmdbGenreId из истории просмотров пользователя.
     * Спрашивает TMDb Discover API, исключает уже посмотренные фильмы.
     *
     * @param topGenreIds  список TMDb genre_id (до 3 штук)
     * @param excludeTmdbIds фильмы которые уже смотрел пользователь
     * @param limit        макс. количество результатов
     */
    public List<FilmDto> getRecommendationsByGenreIds(List<Integer> topGenreIds,
                                                       java.util.Set<Long> excludeTmdbIds,
                                                       int limit) {
        if (topGenreIds == null || topGenreIds.isEmpty()) {
            // Если жанров нет — возвращаем популярные с TMDb
            return convertList(client.discover(null, null, null, null, 1))
                    .stream().limit(limit).collect(Collectors.toList());
        }

        java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
        List<FilmDto> result = new java.util.ArrayList<>();

        // Проходимся по каждому жанру из топов
        for (Integer genreId : topGenreIds) {
            if (result.size() >= limit) break;
            List<FilmDto> candidates = convertList(client.discoverByGenre(genreId, 1));
            for (FilmDto f : candidates) {
                if (result.size() >= limit) break;
                Long tid = f.getTmdbId();
                if (tid != null && !excludeTmdbIds.contains(tid) && seen.add(tid)) {
                    result.add(f);
                }
            }
        }

        // Добираем до limit если не хватило
        if (result.size() < limit) {
            List<FilmDto> popular = convertList(client.discover(null, null, null, null, 1));
            for (FilmDto f : popular) {
                if (result.size() >= limit) break;
                Long tid = f.getTmdbId();
                if (tid != null && !excludeTmdbIds.contains(tid) && seen.add(tid)) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    /**
     * Разрешает имя жанра по TMDb genre_id.
     */
    public String resolveGenreName(Integer genreId) {
        if (genreCache.isEmpty()) warmupGenreCache();
        return genreCache.getOrDefault(genreId, "Прочее");
    }

    /**
     * Обратный поиск: находит TMDb genre_id по русскому имени жанра.
     * Используется в getRecommendations для перевода имени жанра в id.
     */
    public Integer resolveGenreIdByName(String name) {
        if (genreCache.isEmpty()) warmupGenreCache();
        if (name == null) return null;
        return genreCache.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(name.trim()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    // ========================
    // Конвертация
    // ========================

    /**
     * TMDb movie → наш FilmDto.
     *
     * Маппинг полей:
     *   tmdb.id           → dto.tmdbId
     *   tmdb.title        → dto.title
     *   tmdb.originalTitle→ dto.originalTitle
     *   tmdb.releaseDate  → dto.year  (первые 4 символа)
     *   tmdb.genres[0]    → dto.genre (первый жанр как строка)
     *   tmdb.credits.crew → dto.director (job="Director")
     *   tmdb.overview     → dto.description
     *   tmdb.voteAverage  → dto.rating
     *   tmdb.runtime      → dto.durationMinutes
     *   tmdb.posterPath   → dto.posterUrl  (с базовым URL)
     *   tmdb.originalLang → dto.language
     *   tmdb.country[0]   → dto.country
     */
    public FilmDto toFilmDto(TmdbMovieDto movie) {
        FilmDto dto = new FilmDto();

        dto.setTmdbId(movie.getId());

        // id: используем tmdbId как локальный id (фронтенд получит его)
        // Фактически для TMDb-фильмов local DB id = null
        dto.setId(null);

        dto.setTitle(movie.getTitle() != null ? movie.getTitle() : movie.getOriginalTitle());
        dto.setOriginalTitle(movie.getOriginalTitle());
        dto.setYear(movie.getYear());
        dto.setDescription(movie.getOverview());
        dto.setRating(movie.getVoteAverage());
        dto.setDurationMinutes(movie.getRuntime());
        dto.setLanguage(movie.getOriginalLanguage());

        // Постер
        if (movie.getPosterPath() != null) {
            dto.setPosterUrl(imageBaseUrl + movie.getPosterPath());
        }

        // Жанр: из детального запроса — поле genres; из списка — genre_ids
        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            dto.setGenre(movie.getGenres().get(0).getName());
            // Все жанры через запятую в поле tags
            String allGenres = movie.getGenres().stream()
                    .map(TmdbGenreDto::getName)
                    .collect(Collectors.joining(", "));
            dto.setTags(allGenres);
        } else if (movie.getGenreIds() != null && !movie.getGenreIds().isEmpty()) {
            dto.setGenre(resolveGenreName(movie.getGenreIds().get(0)));
            String allGenres = movie.getGenreIds().stream()
                    .map(this::resolveGenreName)
                    .collect(Collectors.joining(", "));
            dto.setTags(allGenres);
        }

        // Режиссёр (только из детального запроса с credits)
        if (movie.getCredits() != null) {
            dto.setDirector(movie.getCredits().findDirector());
        }

        // Страна производства
        if (movie.getProductionCountries() != null && !movie.getProductionCountries().isEmpty()) {
            dto.setCountry(movie.getProductionCountries().get(0).getName());
        }

        return dto;
    }

    // ========================
    // Вспомогательные
    // ========================

    private List<FilmDto> convertList(TmdbPageResponse response) {
        if (response == null || response.getResults() == null) return Collections.emptyList();
        // Нужно знать жанры до конвертации списка
        if (genreCache.isEmpty()) warmupGenreCache();
        return response.getResults().stream()
                .map(this::toFilmDto)
                .collect(Collectors.toList());
    }

    /** Запрашивает список жанров у TMDb и заполняет кэш. */
    private synchronized void warmupGenreCache() {
        if (!genreCache.isEmpty()) return;
        List<TmdbGenreDto> genres = client.getGenreList();
        genres.forEach(g -> genreCache.put(g.getId(), g.getName()));
    }
}
