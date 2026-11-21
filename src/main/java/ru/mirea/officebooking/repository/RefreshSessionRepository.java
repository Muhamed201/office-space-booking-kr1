package ru.mirea.officebooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.officebooking.domain.RefreshSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    Optional<RefreshSession> findByRefreshToken(String refreshToken);

    @Query("select s.user.id from RefreshSession s where s.refreshToken = :token")
    Optional<UUID> findOwnerId(String token);

    List<RefreshSession> findByUserIdAndRevokedFalse(UUID userId);
}
