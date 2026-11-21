package ru.mirea.officebooking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.officebooking.domain.Role;
import ru.mirea.officebooking.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "employee")
    Optional<UserAccount> findById(UUID id);

    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRoleAndActiveTrue(Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserAccount> findLockedByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findLockedById(UUID id);
}
