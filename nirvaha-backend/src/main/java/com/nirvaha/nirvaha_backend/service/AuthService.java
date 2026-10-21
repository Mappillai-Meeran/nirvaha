package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.User;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.nirvaha.nirvaha_backend.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       com.nirvaha.nirvaha_backend.repository.PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        // Legacy SHA-256 migration check
        if (!matches && isSha256Hash(user.getPasswordHash())) {
            String inputSha256 = hashSha256(request.getPassword());
            if (inputSha256.equalsIgnoreCase(user.getPasswordHash())) {
                matches = true;
                // Transparently migrate password hash to BCrypt
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);
            }
        }

        if (!matches) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(token, UserResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getAuthenticatedUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public String initiateForgotPassword(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            // Constant-time dummy operation to prevent timing attacks / account enumeration
            hashSha256("dummy-account-timing-mitigation-" + normalizedEmail);
            return null;
        }

        // Generate cryptographically secure 256-bit random token (64 hex characters)
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);
        String tokenHash = hashSha256(rawToken);

        // Standard 15-minute token lifetime
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);
        com.nirvaha.nirvaha_backend.entity.PasswordResetToken resetToken =
                new com.nirvaha.nirvaha_backend.entity.PasswordResetToken(user, tokenHash, expiryDate);
        passwordResetTokenRepository.save(resetToken);

        // Return raw token for internal service callers / integration testing.
        // The REST controller NEVER exposes this token in API responses.
        return rawToken;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired password reset token");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        String rawToken = request.getToken().trim();
        String tokenHash = hashSha256(rawToken);

        com.nirvaha.nirvaha_backend.entity.PasswordResetToken resetToken =
                passwordResetTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Password reset token has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
    }

    private boolean isSha256Hash(String hash) {
        return hash != null && hash.length() == 64 && hash.matches("^[a-fA-F0-9]+$");
    }

    private String hashSha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing error", e);
        }
    }
}
