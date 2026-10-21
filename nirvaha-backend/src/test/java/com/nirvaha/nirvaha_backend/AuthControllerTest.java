package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.security.JwtService;
import com.nirvaha.nirvaha_backend.service.AuthService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.ResolutionRepository resolutionRepository;
    @Autowired
    private com.nirvaha.nirvaha_backend.repository.IncidentActionRepository incidentActionRepository;
    @Autowired
    private com.nirvaha.nirvaha_backend.repository.EvidenceRepository evidenceRepository;
    @Autowired
    private com.nirvaha.nirvaha_backend.repository.IncidentRepository incidentRepository;
    @Autowired
    private com.nirvaha.nirvaha_backend.repository.UserRepository userRepository;
    @Autowired
    private com.nirvaha.nirvaha_backend.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldLoginSuccessfullyWithValidBCryptCredentials() {
        CreateUserRequest userReq = new CreateUserRequest(
                "Officer User",
                "officer.auth@puducherry.gov.in",
                "securePassword123",
                UserRole.OFFICER
        );
        userService.createUser(userReq);

        LoginRequest loginReq = new LoginRequest("officer.auth@puducherry.gov.in", "securePassword123");
        LoginResponse response = authService.login(loginReq);

        assertThat(response.getToken()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("officer.auth@puducherry.gov.in");
        assertThat(response.getUser().getRole()).isEqualTo(UserRole.OFFICER);

        String emailFromToken = jwtService.extractEmail(response.getToken());
        assertThat(emailFromToken).isEqualTo("officer.auth@puducherry.gov.in");
    }

    @Test
    void shouldFailLoginWithInvalidPassword() {
        CreateUserRequest userReq = new CreateUserRequest(
                "Citizen User",
                "citizen.auth@example.com",
                "realPassword",
                UserRole.CITIZEN
        );
        userService.createUser(userReq);

        LoginRequest loginReq = new LoginRequest("citizen.auth@example.com", "wrongPassword");

        assertThatThrownBy(() -> authService.login(loginReq))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldInitiateAndCompletePasswordReset() {
        CreateUserRequest userReq = new CreateUserRequest(
                "Reset User",
                "reset.user@example.com",
                "oldPassword123",
                UserRole.CITIZEN
        );
        userService.createUser(userReq);

        String resetToken = authService.initiateForgotPassword("reset.user@example.com");
        assertThat(resetToken).isNotNull();

        ResetPasswordRequest resetReq = new ResetPasswordRequest(resetToken, "brandNewPassword123");
        authService.resetPassword(resetReq);

        LoginResponse loginResponse = authService.login(new LoginRequest("reset.user@example.com", "brandNewPassword123"));
        assertThat(loginResponse.getToken()).isNotNull();
    }

    @Test
    void shouldValidateJwtSubjectAndClaims() {
        String token = jwtService.generateToken(42L, "jwt.test@puducherry.gov.in", "ADMIN");
        assertThat(jwtService.extractEmail(token)).isEqualTo("jwt.test@puducherry.gov.in");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.isTokenValid(token, "jwt.test@puducherry.gov.in")).isTrue();
    }
}
