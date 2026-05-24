package by.bsuir.filmcatalog.kp;

import by.bsuir.filmcatalog.dto.kp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;

/**
 * Низкоуровневый HTTP-клиент для Kinopoisk API Unofficial.
 * Base URL: https://kinopoiskapiunofficial.tech
 * Аутентификация: X-API-KEY header
 *
 * Все запросы синхронные (.block()) — проект не реактивный.
 */
@Component
public class KinopoiskClient {

    private static final Logger log = LoggerFactory.getLogger(KinopoiskClient.class);

    private final WebClient webClient;

    public KinopoiskClient(
            @Value("${kp.api.base-url:https://kinopoiskapiunofficial.tech}") String baseUrl,
            @Value("${kp.api.key}") String apiKey
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // ──────────────────────────────────────────────────────────
    // Топ/популярные фильмы
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/v2.2/films/top?type=TOP_100_POPULAR_FILMS&page=1
     * Популярные фильмы (аналог TMDb /movie/popular).
     */
    public KpSearchResponse getPopular(int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/api/v2.2/films/top")
                            .queryParam("type", "TOP_100_POPULAR_FILMS")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(KpSearchResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP getPopular error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return emptySearch();
        } catch (Exception e) {
            log.error("KP getPopular unexpected error", e);
            return emptySearch();
        }
    }

    /**
     * GET /api/v2.2/films/top?type=TOP_250_BEST_FILMS&page=1
     * Топ-250 лучших фильмов (аналог TMDb /movie/top_rated).
     */
    public KpSearchResponse getTop250(int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/api/v2.2/films/top")
                            .queryParam("type", "TOP_250_BEST_FILMS")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(KpSearchResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP getTop250 error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return emptySearch();
        } catch (Exception e) {
            log.error("KP getTop250 unexpected error", e);
            return emptySearch();
        }
    }

    // ──────────────────────────────────────────────────────────
    // Поиск по названию
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/v2.1/films/search-by-keyword?keyword=...&page=1
     */
    public KpSearchResponse searchByKeyword(String keyword, int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/api/v2.1/films/search-by-keyword")
                            .queryParam("keyword", keyword)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(KpSearchResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP searchByKeyword error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return emptySearch();
        } catch (Exception e) {
            log.error("KP searchByKeyword unexpected error", e);
            return emptySearch();
        }
    }

    // ──────────────────────────────────────────────────────────
    // Детали фильма
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/v2.2/films/{id}
     */
    public KpFilmDto getFilmById(Long kpId) {
        try {
            return webClient.get()
                    .uri("/api/v2.2/films/{id}", kpId)
                    .retrieve()
                    .bodyToMono(KpFilmDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP getFilmById({}) error: {} {}", kpId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("KP getFilmById unexpected error", e);
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────
    // Съёмочная группа (для режиссёра)
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/v1/staff?filmId={id}
     * Возвращает актёров, режиссёров и т.д.
     */
    public List<KpStaffResponse> getStaff(Long kpId) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/api/v1/staff")
                            .queryParam("filmId", kpId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<KpStaffResponse>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP getStaff({}) error: {} {}", kpId, e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("KP getStaff unexpected error", e);
            return Collections.emptyList();
        }
    }

    // ──────────────────────────────────────────────────────────
    // Похожие фильмы
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/v2.2/films/{id}/similars
     */
    public KpSimilarsResponse getSimilars(Long kpId) {
        try {
            return webClient.get()
                    .uri("/api/v2.2/films/{id}/similars", kpId)
                    .retrieve()
                    .bodyToMono(KpSimilarsResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP getSimilars({}) error: {}", kpId, e.getStatusCode());
            return emptySimilars();
        } catch (Exception e) {
            log.error("KP getSimilars unexpected error", e);
            return emptySimilars();
        }
    }

    // ──────────────────────────────────────────────────────────
    // Поиск по режиссёру
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/v1/persons?name=...&page=1
     * Поиск персоны (режиссёра) по имени.
     */
    public KpPersonResponse searchPersons(String name) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/api/v1/persons")
                            .queryParam("name", name)
                            .build())
                    .retrieve()
                    .bodyToMono(KpPersonResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP searchPersons error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return emptyPersons();
        } catch (Exception e) {
            log.error("KP searchPersons unexpected error", e);
            return emptyPersons();
        }
    }

    /**
     * GET /api/v2.2/films?staffId={personId}&page=1
     * Фильмы конкретного человека (режиссёра).
     */
    public KpSearchResponse getFilmsByStaff(Long staffId, int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/api/v2.2/films")
                            .queryParam("staffId", staffId)
                            .queryParam("order", "NUM_VOTE")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(KpSearchResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP getFilmsByStaff({}) error: {} {}", staffId, e.getStatusCode(), e.getResponseBodyAsString());
            return emptySearch();
        } catch (Exception e) {
            log.error("KP getFilmsByStaff unexpected error", e);
            return emptySearch();
        }
    }

    // ──────────────────────────────────────────────────────────
    // Фильмы по жанру (для рекомендаций)
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/v2.2/films?genres={genreId}&order=RATING&page=1
     */
    public KpSearchResponse getFilmsByGenre(Integer genreId, int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/api/v2.2/films")
                            .queryParam("genres", genreId)
                            .queryParam("order", "RATING")
                            .queryParam("type", "FILM")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(KpSearchResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("KP getFilmsByGenre({}) error: {} {}", genreId, e.getStatusCode(), e.getResponseBodyAsString());
            return emptySearch();
        } catch (Exception e) {
            log.error("KP getFilmsByGenre unexpected error", e);
            return emptySearch();
        }
    }

    // ──────────────────────────────────────────────────────────
    // Жанры
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/v2.2/films/filters — список жанров и стран с id.
     */
    public KpFiltersResponse getFilters() {
        try {
            return webClient.get()
                    .uri("/api/v2.2/films/filters")
                    .retrieve()
                    .bodyToMono(KpFiltersResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("KP getFilters error", e);
            return new KpFiltersResponse();
        }
    }

    // ──────────────────────────────────────────────────────────
    // Вспомогательные
    // ──────────────────────────────────────────────────────────

    private KpSearchResponse emptySearch() {
        return new KpSearchResponse();
    }

    private KpSimilarsResponse emptySimilars() {
        return new KpSimilarsResponse();
    }

    private KpPersonResponse emptyPersons() {
        return new KpPersonResponse();
    }
}
