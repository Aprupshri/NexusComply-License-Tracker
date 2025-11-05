package com.prodapt.license_tracker_backend.repository;

import com.prodapt.license_tracker_backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public Optional<User> findByUsername(String username);
    public Optional<User> findByEmail(String email);
    public Boolean existsByUsername(String username);

    public Boolean existsByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
}
