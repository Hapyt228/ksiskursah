package by.bsuir.filmcatalog.repository;

import by.bsuir.filmcatalog.model.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link Film}.
 * Предоставляет методы поиска и фильтрации через Spring Data JPA.
 */
@Repository
public interface FilmRepository extends JpaRepository<Film, Long> {

    /**
     * Найти локальную копию TMDb-фильма по его tmdbId.
     * Используется при записи просмотра: если фильм уже есть в БД — берём его,
     * иначе сохраняем новый.
     */
    Optional<Film> findByTmdbId(Long tmdbId);

    /**
     * Полнотекстовый поиск по названию, режиссёру и описанию.
     * Регистронезависимый поиск.
     */
    @Query("SELECT f FROM Film f WHERE " +
           "LOWER(f.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.originalTitle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.director) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Film> searchByQuery(@Param("query") String query);

    /**
     * Фильтрация по жанру (регистронезависимо).
     */
    List<Film> findByGenreIgnoreCase(String genre);

    /**
     * Фильтрация по диапазону годов.
     */
    List<Film> findByYearBetween(Integer yearFrom, Integer yearTo);

    /**
     * Фильтрация по минимальному рейтингу.
     */
    List<Film> findByRatingGreaterThanEqualOrderByRatingDesc(Double minRating);

    /**
     * Фильтрация по стране производства.
     */
    List<Film> findByCountryIgnoreCase(String country);

    /**
     * Фильтрация по режиссёру.
     */
    List<Film> findByDirectorIgnoreCaseContaining(String director);

    /**
     * Получить все уникальные жанры (для фильтров UI).
     */
    @Query("SELECT DISTINCT f.genre FROM Film f ORDER BY f.genre")
    List<String> findAllDistinctGenres();

    /**
     * Получить все уникальные страны.
     */
    @Query("SELECT DISTINCT f.country FROM Film f WHERE f.country IS NOT NULL ORDER BY f.country")
    List<String> findAllDistinctCountries();

    /**
     * Топ фильмов по рейтингу (для рекомендаций).
     */
    @Query("SELECT f FROM Film f ORDER BY f.rating DESC LIMIT :limit")
    List<Film> findTopRated(@Param("limit") int limit);

    /**
     * Рекомендации: фильмы того же жанра, исключая текущий.
     */
    @Query("SELECT f FROM Film f WHERE f.genre = :genre AND f.id <> :excludeId ORDER BY f.rating DESC LIMIT 6")
    List<Film> findSimilarByGenre(@Param("genre") String genre, @Param("excludeId") Long excludeId);

    /**
     * Рекомендации: фильмы того же режиссёра, исключая текущий.
     */
    @Query("SELECT f FROM Film f WHERE f.director = :director AND f.id <> :excludeId ORDER BY f.rating DESC LIMIT 4")
    List<Film> findByDirectorExcluding(@Param("director") String director, @Param("excludeId") Long excludeId);

    /**
     * Комбинированная фильтрация.
     */
    @Query("SELECT f FROM Film f WHERE " +
           "(:genre IS NULL OR LOWER(f.genre) = LOWER(:genre)) AND " +
           "(:yearFrom IS NULL OR f.year >= :yearFrom) AND " +
           "(:yearTo IS NULL OR f.year <= :yearTo) AND " +
           "(:minRating IS NULL OR f.rating >= :minRating) AND " +
           "(:country IS NULL OR LOWER(f.country) = LOWER(:country)) " +
           "ORDER BY f.rating DESC")
    List<Film> findByFilters(
            @Param("genre") String genre,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo,
            @Param("minRating") Double minRating,
            @Param("country") String country
    );
}
