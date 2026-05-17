package by.bsuir.filmcatalog.tmdb;

import by.bsuir.filmcatalog.dto.tmdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;

/**
 * Низкоуровневый HTTP-клиент для TMDb API v3.
 *
 * Все запросы выполняются синхронно (.block()), поскольку
 * остальной проект не использует реактивный подход.
 *
 * Ключ передаётся как Bearer token в заголовке Authorization
 * (TMDb рекомендует именно этот способ для v3).
 */
@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

    private final WebClient webClient;

    @Value("${tmdb.api.language:ru-RU}")
    private String language;

    public TmdbClient(
            @Value("${tmdb.api.base-url}") String baseUrl,
            @Value("${tmdb.api.token}") String token
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // ========================
    // Популярные фильмы
    // ========================

    /**
     * GET /movie/popular?language=ru-RU&page=1
     */
    public TmdbPageResponse getPopularMovies(int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/movie/popular")
                            .queryParam("language", language)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(TmdbPageResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("TMDb getPopularMovies error: {} {}", e.getStatusCode(), e.getMessage());
            return emptyPage();
        } catch (Exception e) {
            log.error("TMDb getPopularMovies unexpected error", e);
            return emptyPage();
        }
    }

    // ========================
    // Поиск фильмов
    // ========================

    /**
     * GET /search/movie?query=...&language=ru-RU&page=1
     */
    public TmdbPageResponse searchMovies(String query, int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/search/movie")
                            .queryParam("query", query)
                            .queryParam("language", language)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(TmdbPageResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("TMDb searchMovies error: {} {}", e.getStatusCode(), e.getMessage());
            return emptyPage();
        } catch (Exception e) {
            log.error("TMDb searchMovies unexpected error", e);
            return emptyPage();
        }
    }

    // ========================
    // Детали фильма
    // ========================

    /**
     * GET /movie/{id}?language=ru-RU&append_to_response=credits
     * append_to_response=credits позволяет за один запрос получить и режиссёра.
     */
    public TmdbMovieDto getMovieDetails(Long tmdbId) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/movie/{id}")
                            .queryParam("language", language)
                            .queryParam("append_to_response", "credits")
                            .build(tmdbId))
                    .retrieve()
                    .bodyToMono(TmdbMovieDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("TMDb getMovieDetails({}) error: {} {}", tmdbId, e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("TMDb getMovieDetails unexpected error", e);
            return null;
        }
    }

    // ========================
    // Список жанров
    // ========================

    /**
     * GET /genre/movie/list?language=ru-RU
     */
    public List<TmdbGenreDto> getGenreList() {
        try {
            TmdbGenreListResponse response = webClient.get()
                    .uri(u -> u.path("/genre/movie/list")
                            .queryParam("language", language)
                            .build())
                    .retrieve()
                    .bodyToMono(TmdbGenreListResponse.class)
                    .block();
            return response != null && response.getGenres() != null
                    ? response.getGenres()
                    : Collections.emptyList();
        } catch (Exception e) {
            log.error("TMDb getGenreList error", e);
            return Collections.emptyList();
        }
    }

    // ========================
    // Рекомендации по фильму
    // ========================

    /**
     * GET /movie/{id}/recommendations?language=ru-RU&page=1
     * Используется для блока "Похожие фильмы" на странице фильма.
     */
    public TmdbPageResponse getMovieRecommendations(Long tmdbId, int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/movie/{id}/recommendations")
                            .queryParam("language", language)
                            .queryParam("page", page)
                            .build(tmdbId))
                    .retrieve()
                    .bodyToMono(TmdbPageResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("TMDb getMovieRecommendations({}) error: {}", tmdbId, e.getStatusCode());
            return emptyPage();
        } catch (Exception e) {
            log.error("TMDb getMovieRecommendations unexpected error", e);
            return emptyPage();
        }
    }

    // ========================
    // Фильмы по жанру (для рекомендаций)
    // ========================

    /**
     * GET /discover/movie — универсальный Discover с фильтрами.
     *
     * @param genreId        ID жанра в TMDb (nullable — если null, жанр не фильтруется)
     * @param yearFrom       мин. год выхода (nullable)
     * @param yearTo         макс. год выхода (nullable)
     * @param voteAverageGte мин. рейтинг (nullable)
     * @param page           страница (1-500)
     */
    public TmdbPageResponse discover(Integer genreId, Integer yearFrom, Integer yearTo,
                                     Double voteAverageGte, int page) {
        try {
            return webClient.get()
                    .uri(u -> {
                        var b = u.path("/discover/movie")
                                .queryParam("sort_by", "vote_average.desc")
                                .queryParam("vote_count.gte", "100")
                                .queryParam("language", language)
                                .queryParam("page", page);
                        if (genreId != null)        b = b.queryParam("with_genres", genreId);
                        if (yearFrom != null)        b = b.queryParam("primary_release_date.gte", yearFrom + "-01-01");
                        if (yearTo   != null)        b = b.queryParam("primary_release_date.lte", yearTo   + "-12-31");
                        if (voteAverageGte != null)  b = b.queryParam("vote_average.gte", voteAverageGte);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(TmdbPageResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("TMDb discover error (genre={}, yearFrom={}, yearTo={}, rating={})",
                    genreId, yearFrom, yearTo, voteAverageGte, e);
            return emptyPage();
        }
    }

    /**
     * Устаревший метод — оставлен для совместимости с UserFilmService.
     * Используй discover() напрямую.
     */
    public TmdbPageResponse discoverByGenre(Integer genreId, int page) {
        return discover(genreId, null, null, null, page);
    }

    // ========================
    // Вспомогательные
    // ========================

    private TmdbPageResponse emptyPage() {
        TmdbPageResponse r = new TmdbPageResponse();
        r.setResults(Collections.emptyList());
        r.setPage(1);
        r.setTotalPages(0);
        r.setTotalResults(0);
        return r;
    }
}
