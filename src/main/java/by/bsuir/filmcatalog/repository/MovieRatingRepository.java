package by.bsuir.filmcatalog.repository;

import by.bsuir.filmcatalog.model.MovieRating;
import by.bsuir.filmcatalog.model.User;
import by.bsuir.filmcatalog.model.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRatingRepository extends JpaRepository<MovieRating, Long> {

    Optional<MovieRating> findByUserAndFilm(User user, Film film);

    List<MovieRating> findByUser(User user);

    /**
     * Средняя оценка фильма всеми пользователями.
     */
    @Query("SELECT AVG(r.stars) FROM MovieRating r WHERE r.film = :film")
    Double getAverageRatingForFilm(@Param("film") Film film);

    /**
     * Жанры с весами по оценкам (для рекомендаций).
     * 5 звёзд = вес 5, 4 = вес 3, 3 = вес 1, ниже — не учитываем
     */
    @Query("SELECT r.film.genre, SUM(CASE " +
           "WHEN r.stars = 5 THEN 5 " +
           "WHEN r.stars = 4 THEN 3 " +
           "WHEN r.stars = 3 THEN 1 " +
           "ELSE 0 END) " +
           "FROM MovieRating r WHERE r.user = :user AND r.stars >= 3 " +
           "GROUP BY r.film.genre ORDER BY 2 DESC")
    List<Object[]> findWeightedGenresByRating(@Param("user") User user);
}
