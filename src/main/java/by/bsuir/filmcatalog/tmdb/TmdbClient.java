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

// HTTP клиент для работы с TMDb API.
// Запросы синхронные (.block()), токен передаётся в заголовке Authorization.
@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

    private final WebClient webClient;

    @Value("${tmdb.api.language:ru-RU}")
    private String language;

    public TmdbClient(
            @Value("${tmdb.api.base-url}") String baseUrl,
            @Value("${tmdb.api.token}") String token) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // GET /movie/popular
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
            log.error("TMDb getPopularMovies: {} {}", e.getStatusCode(), e.getMessage());
            return emptyPage();
        } catch (Exception e) {
            log.error("TMDb getPopularMovies error", e);
            return emptyPage();
        }
    }

    // GET /movie/top_rated
    public TmdbPageResponse getTopRatedMovies(int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/movie/top_rated")
                            .queryParam("language", language)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(TmdbPageResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("TMDb getTopRatedMovies: {} {}", e.getStatusCode(), e.getMessage());
            return emptyPage();
        } catch (Exception e) {
            log.error("TMDb getTopRatedMovies error", e);
            return emptyPage();
        }
    }

    // GET /search/movie?query=...
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
            log.error("TMDb searchMovies: {} {}", e.getStatusCode(), e.getMessage());
            return emptyPage();
        } catch (Exception e) {
            log.error("TMDb searchMovies error", e);
            return emptyPage();
        }
    }

    // GET /movie/{id}?append_to_response=credits
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
            log.error("TMDb getMovieDetails({}): {} {}", tmdbId, e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("TMDb getMovieDetails error", e);
            return null;
        }
    }

    // GET /genre/movie/list
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

    // GET /movie/{id}/recommendations — похожие фильмы
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
            log.error("TMDb getMovieRecommendations({}): {}", tmdbId, e.getStatusCode());
            return emptyPage();
        } catch (Exception e) {
            log.error("TMDb getMovieRecommendations error", e);
            return emptyPage();
        }
    }

    // GET /discover/movie — универсальная фильтрация фильмов
    public TmdbPageResponse discover(Integer genreId, Integer yearFrom, Integer yearTo,
                                     Double voteAverageGte, int page) {
        try {
            return webClient.get()
                    .uri(u -> {
                        var b = u.path("/discover/movie")
                                .queryParam("language", language)
                                .queryParam("page", page);

                        // Порог голосов зависит от фильтра рейтинга
                        if (voteAverageGte != null && voteAverageGte >= 8.5) {
                            b = b.queryParam("vote_count.gte", "50")
                                 .queryParam("sort_by", "vote_average.desc");
                        } else {
                            b = b.queryParam("vote_count.gte", "200")
                                 .queryParam("sort_by", "vote_average.desc");
                        }

                        if (genreId != null)       b = b.queryParam("with_genres", genreId);
                        if (yearFrom != null)       b = b.queryParam("primary_release_date.gte", yearFrom + "-01-01");
                        if (yearTo != null)         b = b.queryParam("primary_release_date.lte", yearTo + "-12-31");
                        if (voteAverageGte != null) b = b.queryParam("vote_average.gte", voteAverageGte);

                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(TmdbPageResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("TMDb discover error", e);
            return emptyPage();
        }
    }

    // Обёртка для совместимости со старым кодом
    public TmdbPageResponse discoverByGenre(Integer genreId, int page) {
        return discover(genreId, null, null, null, page);
    }

    // GET /search/person?query=...
    public TmdbPersonPageResponse searchPersons(String query) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/search/person")
                            .queryParam("query", query)
                            .queryParam("language", language)
                            .build())
                    .retrieve()
                    .bodyToMono(TmdbPersonPageResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("TMDb searchPersons: {} {}", e.getStatusCode(), e.getMessage());
            return emptyPersonPage();
        } catch (Exception e) {
            log.error("TMDb searchPersons error", e);
            return emptyPersonPage();
        }
    }

    // GET /discover/movie?with_crew={personId} — фильмы конкретного режиссёра
    public TmdbPageResponse discoverByDirector(Long personId, int page) {
        try {
            return webClient.get()
                    .uri(u -> u.path("/discover/movie")
                            .queryParam("language", language)
                            .queryParam("with_crew", personId)
                            .queryParam("sort_by", "vote_count.desc")
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .bodyToMono(TmdbPageResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("TMDb discoverByDirector({}): {}", personId, e.getStatusCode());
            return emptyPage();
        } catch (Exception e) {
            log.error("TMDb discoverByDirector error", e);
            return emptyPage();
        }
    }

    private TmdbPageResponse emptyPage() {
        TmdbPageResponse r = new TmdbPageResponse();
        r.setResults(Collections.emptyList());
        r.setPage(1);
        r.setTotalPages(0);
        r.setTotalResults(0);
        return r;
    }

    private TmdbPersonPageResponse emptyPersonPage() {
        TmdbPersonPageResponse r = new TmdbPersonPageResponse();
        r.setResults(Collections.emptyList());
        r.setTotalResults(0);
        return r;
    }
}
