package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.PasswordResetToken;
import com.nirvaha.nirvaha_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserOrderByCreatedAtDesc(User user);

    void deleteByExpiryDateBefore(LocalDateTime expiryDate);
}
