package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.PasswordResetToken;
import com.nirvaha.nirvaha_backend.entity.User;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.repository.PasswordResetTokenRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.AuthService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PasswordRecoverySecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.security.RateLimitingFilter rateLimitingFilter;

    private static final String TEST_EMAIL = "citizen.recovery@example.com";
    private static final String INITIAL_PASSWORD = "InitialPassword123!";
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        SecurityContextHolder.clearContext();
        rateLimitingFilter.clearBuckets();
        cleanupTestUser();

        UserResponse response = userService.createUser(new CreateUserRequest(
                "Recovery Test Citizen",
                TEST_EMAIL,
                INITIAL_PASSWORD,
                UserRole.CITIZEN
        ));
        testUser = userRepository.findById(response.getId()).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        cleanupTestUser();
    }

    private void cleanupTestUser() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
            passwordResetTokenRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .forEach(passwordResetTokenRepository::delete);
            userRepository.delete(user);
        });
    }

    @Test
    @DisplayName("forgot-password never returns usable reset token in API response payload")
    void forgotPasswordNeverReturnsTokenInResponse() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("password reset instructions have been initiated")))
                .andExpect(jsonPath("$.resetToken").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("forgot-password returns consistent response for known and unknown accounts to reduce account enumeration")
    void forgotPasswordUnknownAccountReturnsSameGenericResponse() throws Exception {
        MvcResult knownResult = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").doesNotExist())
                .andReturn();

        MvcResult unknownResult = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent.ghost.user@puducherry.gov.in\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").doesNotExist())
                .andReturn();

        assertThat(knownResult.getResponse().getContentAsString())
                .isEqualTo(unknownResult.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Valid password reset flow completes and updates password successfully")
    void validResetPasswordSucceeds() throws Exception {
        String rawToken = authService.initiateForgotPassword(TEST_EMAIL);
        assertThat(rawToken).isNotNull();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"NewSecurePassword123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    @DisplayName("Successful password reset enforces new password and invalidates old password for login")
    void passwordUpdateEnforcesNewPassword() throws Exception {
        String rawToken = authService.initiateForgotPassword(TEST_EMAIL);
        assertThat(rawToken).isNotNull();

        // 1. Reset password
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"NewSecurePassword123!\"}"))
                .andExpect(status().isOk());

        // 2. Old password must be rejected
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + INITIAL_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        // 3. New password must authenticate successfully
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"NewSecurePassword123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL));
    }

    @Test
    @DisplayName("Token reuse is strictly blocked after first successful reset")
    void tokenReuseIsBlocked() throws Exception {
        String rawToken = authService.initiateForgotPassword(TEST_EMAIL);
        assertThat(rawToken).isNotNull();

        // First use: Success
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"FirstUpdatePass123!\"}"))
                .andExpect(status().isOk());

        // Second use: Must be rejected with 400 Bad Request
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"SecondUpdatePass123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already been used")));
    }

    @Test
    @DisplayName("Expired token cannot reset password and returns 400 Bad Request")
    void expiredTokenIsRejected() throws Exception {
        String rawToken = "expired_raw_test_token_abcdef1234567890abcdef1234567890abcdef12";
        String tokenHash = calculateSha256(rawToken);

        // Seed token with expiry 10 minutes in the past
        PasswordResetToken expiredToken = new PasswordResetToken(
                testUser,
                tokenHash,
                LocalDateTime.now().minusMinutes(10)
        );
        passwordResetTokenRepository.save(expiredToken);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"BrandNewPass123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    @Test
    @DisplayName("Invalid or tampered token returns 400 Bad Request")
    void invalidOrTamperedTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-bogus-token-random\",\"newPassword\":\"BrandNewPass123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid or expired password reset token")));
    }

    @Test
    @DisplayName("Rate limiting rejects recovery attempts after exceeding threshold (HTTP 429)")
    void rateLimitingPreventsAbuseOnRecoveryEndpoints() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                    .andExpect(status().isOk());
        }

        // 6th attempt should be blocked with 429 Too Many Requests
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(containsString("Rate limit exceeded for password recovery")));
    }

    private String calculateSha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
