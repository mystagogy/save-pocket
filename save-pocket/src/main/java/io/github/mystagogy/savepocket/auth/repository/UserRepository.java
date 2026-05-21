package io.github.mystagogy.savepocket.auth.repository;

import io.github.mystagogy.savepocket.auth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
}
