package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.LoginRequest;
import com.nirvaha.nirvaha_backend.dto.LoginResponse;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.AuthService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class IntelligenceSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String citizenToken;
    private String officerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String citizenEmail = "intel_citizen@example.com";
        if (userRepository.findByEmail(citizenEmail).isEmpty()) {
            userService.createUser(new CreateUserRequest("Intel Citizen", citizenEmail, "Password123!", UserRole.CITIZEN));
        }
        LoginResponse citLogin = authService.login(new LoginRequest(citizenEmail, "Password123!"));
        citizenToken = citLogin.getToken();

        String officerEmail = "intel_officer@example.com";
        if (userRepository.findByEmail(officerEmail).isEmpty()) {
            var user = new com.nirvaha.nirvaha_backend.entity.User();
            user.setName("Intel Officer");
            user.setEmail(officerEmail);
            user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Password123!"));
            user.setRole(UserRole.OFFICER);
            userRepository.save(user);
        }
        LoginResponse offLogin = authService.login(new LoginRequest(officerEmail, "Password123!"));
        officerToken = offLogin.getToken();
    }

    @Test
    void unauthenticatedRequestToIntelligenceShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/intelligence/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void citizenRequestToIntelligenceShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/intelligence/overview")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void officerRequestToIntelligenceShouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/api/intelligence/overview")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
    }
}
