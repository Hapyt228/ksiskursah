package by.bsuir.filmcatalog.repository;

import by.bsuir.filmcatalog.model.MovieUserStatus;
import by.bsuir.filmcatalog.model.User;
import by.bsuir.filmcatalog.model.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieUserStatusRepository extends JpaRepository<MovieUserStatus, Long> {

    Optional<MovieUserStatus> findByUserAndFilm(User user, Film film);

    List<MovieUserStatus> findByUser(User user);

    /**
     * Жанры с их суммарным весом (для рекомендаций).
     * Используется в алгоритме рекомендаций.
     */
    @Query("SELECT s.film.genre, SUM(CASE s.status " +
           "WHEN 'FAVOURITE' THEN 5 WHEN 'REWATCHING' THEN 5 " +
           "WHEN 'WATCHING' THEN 3 WHEN 'PLANNED' THEN 3 WHEN 'COMPLETED' THEN 3 " +
           "WHEN 'POSTPONED' THEN 1 ELSE 0 END) " +
           "FROM MovieUserStatus s " +
           "WHERE s.user = :user AND s.status <> 'DROPPED' " +
           "GROUP BY s.film.genre ORDER BY 2 DESC")
    List<Object[]> findWeightedGenresByUser(@Param("user") User user);

    /** film_id, уже имеющие статус у пользователя */
    @Query("SELECT s.film.id FROM MovieUserStatus s WHERE s.user = :user")
    List<Long> findFilmIdsByUser(@Param("user") User user);
}
