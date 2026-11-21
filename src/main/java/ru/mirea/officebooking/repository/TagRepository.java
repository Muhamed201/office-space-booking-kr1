package ru.mirea.officebooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.officebooking.domain.Tag;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);

    @Modifying
    @Query(value = "insert into tag(name, color) values (:name, '#3B82F6') on conflict (name) do nothing",
            nativeQuery = true)
    void ensureExists(String name);
}
