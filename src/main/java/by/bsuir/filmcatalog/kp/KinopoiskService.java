package by.bsuir.filmcatalog.kp;

import by.bsuir.filmcatalog.dto.FilmDto;
import by.bsuir.filmcatalog.dto.kp.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис для работы с Kinopoisk API Unofficial.
 *
 * Полностью заменяет TmdbService — предоставляет те же публичные методы,
 * поэтому UserFilmService и контроллер работают без изменений логики.
 *
 * Маппинг KP → FilmDto:
 *   kp.kinopoiskId  → dto.tmdbId   (переиспользуем поле как "внешний id")
 *   kp.nameRu       → dto.title
 *   kp.nameOriginal → dto.originalTitle
 *   kp.year         → dto.year
 *   kp.description  → dto.description
 *   kp.ratingKinopoisk → dto.rating
 *   kp.filmLength   → dto.durationMinutes
 *   kp.posterUrl    → dto.posterUrl
 *   kp.genres[0]    → dto.genre
 *   kp.countries[0] → dto.country
 */
@Service
public class KinopoiskService {

    private final KinopoiskClient client;

    /** Кэш: genreName (lowercase) → KP genre id. Заполняется лениво. */
    private final Map<String, Integer> genreNameToId = new ConcurrentHashMap<>();
    /** Кэш: KP genre id → genreName */
    private final Map<Integer, String> genreIdToName = new ConcurrentHashMap<>();

    public KinopoiskService(KinopoiskClient client) {
        this.client = client;
    }

    // ──────────────────────────────────────────────────────────
    // Публичные методы (аналоги TmdbService)
    // ──────────────────────────────────────────────────────────

    public List<FilmDto> getPopular(int page) {
        return convertSearch(client.getPopular(page));
    }

    public List<FilmDto> getTopRated(int page) {
        return convertSearch(client.getTop250(page));
    }

    public List<FilmDto> search(String query, int page) {
        return convertSearch(client.searchByKeyword(query, page));
    }

    /**
     * Детали фильма по KP id + отдельный запрос за режиссёром.
     */
    public FilmDto getDetails(Long kpId) {
        KpFilmDto film = client.getFilmById(kpId);
        if (film == null) return null;

        FilmDto dto = toFilmDto(film);

        // Получаем режиссёра через /api/v1/staff
        List<KpStaffResponse> staff = client.getStaff(kpId);
        if (staff != null) {
            staff.stream()
                    .filter(KpStaffResponse::isDirector)
                    .findFirst()
                    .ifPresent(d -> dto.setDirector(d.getBestName()));
        }
        return dto;
    }

    /**
     * Поиск по режиссёру:
     *   1. /api/v1/persons?name=... — находим персону
     *   2. /api/v2.2/films?staffId=... — берём его фильмы
     */
    public List<FilmDto> searchByDirector(String name) {
        if (name == null || name.isBlank()) return Collections.emptyList();

        KpPersonResponse persons = client.searchPersons(name);
        if (persons == null || persons.getItems() == null || persons.getItems().isEmpty()) {
            return Collections.emptyList();
        }

        KpPersonItem director = persons.getItems().get(0);

        List<FilmDto> page1 = convertSearch(client.getFilmsByStaff(director.getKinopoiskId(), 1));
        List<FilmDto> page2 = convertSearch(client.getFilmsByStaff(director.getKinopoiskId(), 2));

        Set<Long> seen = new LinkedHashSet<>();
        List<FilmDto> result = new ArrayList<>();
        for (FilmDto f : page1) if (f.getTmdbId() != null && seen.add(f.getTmdbId())) result.add(f);
        for (FilmDto f : page2) if (f.getTmdbId() != null && seen.add(f.getTmdbId())) result.add(f);

        result.forEach(f -> f.setDirector(director.getBestName()));
        return result;
    }

    /**
     * Похожие фильмы для страницы фильма.
     */
    public List<FilmDto> getSimilar(Long kpId) {
        KpSimilarsResponse resp = client.getSimilars(kpId);
        return resp.getItems().stream()
                .map(this::itemToFilmDto)
                .collect(Collectors.toList());
    }

    /**
     * Жанры — возвращаем в виде TmdbGenreDto для совместимости с контроллером.
     */
    public List<by.bsuir.filmcatalog.dto.tmdb.TmdbGenreDto> getGenres() {
        ensureGenreCache();
        return genreIdToName.entrySet().stream()
                .map(e -> {
                    var g = new by.bsuir.filmcatalog.dto.tmdb.TmdbGenreDto();
                    g.setId(e.getKey());
                    g.setName(e.getValue());
                    return g;
                })
                .sorted(Comparator.comparing(by.bsuir.filmcatalog.dto.tmdb.TmdbGenreDto::getName))
                .collect(Collectors.toList());
    }

    /**
     * Фильмы по жанру (для рекомендаций).
     */
    public List<FilmDto> getByGenreId(Integer genreId, int limit) {
        return convertSearch(client.getFilmsByGenre(genreId, 1))
                .stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Рекомендации по топ жанрам из истории просмотров.
     * Аналог TmdbService.getRecommendationsByGenreIds().
     */
    public List<FilmDto> getRecommendationsByGenreIds(List<Integer> topGenreIds,
                                                       Set<Long> excludeKpIds,
                                                       int limit) {
        if (topGenreIds == null || topGenreIds.isEmpty()) {
            return convertSearch(client.getPopular(1))
                    .stream().limit(limit).collect(Collectors.toList());
        }

        Set<Long> seen = new LinkedHashSet<>();
        List<FilmDto> result = new ArrayList<>();

        for (Integer genreId : topGenreIds) {
            if (result.size() >= limit) break;
            for (FilmDto f : convertSearch(client.getFilmsByGenre(genreId, 1))) {
                if (result.size() >= limit) break;
                Long kid = f.getTmdbId();
                if (kid != null && !excludeKpIds.contains(kid) && seen.add(kid)) {
                    result.add(f);
                }
            }
        }

        // Добираем до limit популярными если не хватило
        if (result.size() < limit) {
            for (FilmDto f : convertSearch(client.getPopular(1))) {
                if (result.size() >= limit) break;
                Long kid = f.getTmdbId();
                if (kid != null && !excludeKpIds.contains(kid) && seen.add(kid)) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    /**
     * Resolve genre name → genre id (для алгоритма рекомендаций).
     */
    public Integer resolveGenreIdByName(String name) {
        ensureGenreCache();
        if (name == null) return null;
        return genreNameToId.get(name.trim().toLowerCase());
    }

    /**
     * Resolve genre id → genre name.
     */
    public String resolveGenreName(Integer id) {
        ensureGenreCache();
        return genreIdToName.getOrDefault(id, "Прочее");
    }

    // ──────────────────────────────────────────────────────────
    // Конвертация
    // ──────────────────────────────────────────────────────────

    /** KpFilmDto (детали) → FilmDto */
    public FilmDto toFilmDto(KpFilmDto kp) {
        FilmDto dto = new FilmDto();
        dto.setTmdbId(kp.getKinopoiskId());
        dto.setId(null);
        dto.setTitle(kp.getBestName());
        dto.setOriginalTitle(kp.getNameOriginal() != null ? kp.getNameOriginal() : kp.getNameEn());
        dto.setYear(kp.getYear());
        dto.setDescription(kp.getDescription());
        dto.setRating(kp.getBestRating());
        dto.setDurationMinutes(kp.getFilmLength());
        dto.setPosterUrl(kp.getPosterUrl());

        if (kp.getGenres() != null && !kp.getGenres().isEmpty()) {
            dto.setGenre(capitalize(kp.getGenres().get(0).getGenre()));
            dto.setTags(kp.getGenres().stream()
                    .map(g -> capitalize(g.getGenre()))
                    .collect(Collectors.joining(", ")));
        }

        if (kp.getCountries() != null && !kp.getCountries().isEmpty()) {
            dto.setCountry(kp.getCountries().get(0).getCountry());
        }

        return dto;
    }

    /** KpFilmItem (элемент списка) → FilmDto */
    public FilmDto itemToFilmDto(KpFilmItem item) {
        FilmDto dto = new FilmDto();
        dto.setTmdbId(item.getId());
        dto.setId(null);
        dto.setTitle(item.getBestName());
        dto.setOriginalTitle(item.getNameOriginal() != null ? item.getNameOriginal() : item.getNameEn());
        dto.setYear(item.getYearInt());
        dto.setRating(item.getBestRating());
        dto.setPosterUrl(item.getPosterUrl());

        if (item.getGenres() != null && !item.getGenres().isEmpty()) {
            dto.setGenre(capitalize(item.getGenres().get(0).getGenre()));
            dto.setTags(item.getGenres().stream()
                    .map(g -> capitalize(g.getGenre()))
                    .collect(Collectors.joining(", ")));
        }

        if (item.getCountries() != null && !item.getCountries().isEmpty()) {
            dto.setCountry(item.getCountries().get(0).getCountry());
        }

        return dto;
    }

    // ──────────────────────────────────────────────────────────
    // Вспомогательные
    // ──────────────────────────────────────────────────────────

    private List<FilmDto> convertSearch(KpSearchResponse resp) {
        if (resp == null) return Collections.emptyList();
        return resp.getResults().stream()
                .map(this::itemToFilmDto)
                .collect(Collectors.toList());
    }

    private synchronized void ensureGenreCache() {
        if (!genreIdToName.isEmpty()) return;
        KpFiltersResponse filters = client.getFilters();
        if (filters != null && filters.getGenres() != null) {
            for (KpFiltersResponse.KpGenreFilter g : filters.getGenres()) {
                if (g.getId() != null && g.getGenre() != null) {
                    genreIdToName.put(g.getId(), capitalize(g.getGenre()));
                    genreNameToId.put(g.getGenre().trim().toLowerCase(), g.getId());
                    // Также добавляем capitalize вариант
                    genreNameToId.put(capitalize(g.getGenre()).toLowerCase(), g.getId());
                }
            }
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
