package by.bsuir.filmcatalog.repository;

import by.bsuir.filmcatalog.model.MovieViewHistory;
import by.bsuir.filmcatalog.model.User;
import by.bsuir.filmcatalog.model.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovieViewHistoryRepository extends JpaRepository<MovieViewHistory, Long> {

    List<MovieViewHistory> findByUserOrderByViewedAtDesc(User user);

    /** Все film_id которые пользователь уже видел (для исключения из рекомендаций) */
    @Query("SELECT DISTINCT h.film.id FROM MovieViewHistory h WHERE h.user = :user")
    List<Long> findViewedFilmIdsByUser(@Param("user") User user);

    /**
     * Дедупликация: true если за последние N минут уже был просмотр этого фильма.
     * Используется в UserFilmService.recordView() чтобы не спамить в историю.
     */
    boolean existsByUserAndFilmAndViewedAtAfter(User user, Film film, LocalDateTime after);
}
