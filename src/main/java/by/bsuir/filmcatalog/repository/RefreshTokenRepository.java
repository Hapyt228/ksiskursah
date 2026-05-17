package by.bsuir.filmcatalog.repository;

import by.bsuir.filmcatalog.model.RefreshToken;
import by.bsuir.filmcatalog.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** Отзывает все активные refresh-токены пользователя (для logout) */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllByUser(@Param("user") User user);

    /** Удаляет истёкшие токены (для периодической очистки) */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
