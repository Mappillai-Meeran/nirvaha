package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.security.JwtService;
import com.nirvaha.nirvaha_backend.service.EvidenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("dev")
class ProductionConfigurationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EvidenceService evidenceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testActuatorHealthEndpointIsPublicAndSanitized() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void testSecurityHeadersIntact() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testCorsOriginHandling() throws Exception {
        mockMvc.perform(options("/api/incidents")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void testJwtTokenGenerationAndValidation() {
        String token = jwtService.generateToken(1L, "test@example.com", "CITIZEN");
        assertNotNull(token);
        assertEquals("test@example.com", jwtService.extractEmail(token));
        assertEquals(1L, jwtService.extractUserId(token));
        assertEquals("CITIZEN", jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token, "test@example.com"));
    }

    @Test
    void testConfiguredEvidenceStoragePath() {
        assertNotNull(evidenceService.getStoragePath());
        assertFalse(evidenceService.getStoragePath().isBlank());
    }

    @Test
    void testProductionProfileRejectsDevJwtSecret() {
        org.springframework.mock.env.MockEnvironment prodEnv = new org.springframework.mock.env.MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        JwtService prodJwt = new JwtService(prodEnv);
        org.springframework.test.util.ReflectionTestUtils.setField(prodJwt, "secret", "nirvahaSecretKeyForLocalDevelopmentDoNotUseInProductionSuperSecretKey12345!");
        assertThrows(IllegalStateException.class, prodJwt::validateConfiguration);
    }

    @Test
    void testProductionProfileRejectsMissingJwtSecret() {
        org.springframework.mock.env.MockEnvironment prodEnv = new org.springframework.mock.env.MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        JwtService prodJwt = new JwtService(prodEnv);
        org.springframework.test.util.ReflectionTestUtils.setField(prodJwt, "secret", "");
        assertThrows(IllegalStateException.class, prodJwt::validateConfiguration);
    }

    @Test
    void testProductionProfileRejectsShortJwtSecret() {
        org.springframework.mock.env.MockEnvironment prodEnv = new org.springframework.mock.env.MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        JwtService prodJwt = new JwtService(prodEnv);
        org.springframework.test.util.ReflectionTestUtils.setField(prodJwt, "secret", "short-secret-under-32-bytes");
        assertThrows(IllegalStateException.class, prodJwt::validateConfiguration);
    }

    @Test
    void testProductionPropertiesRequireMandatorySecretsWithoutFallback() throws Exception {
        java.util.Properties prodProps = new java.util.Properties();
        try (var is = getClass().getResourceAsStream("/application-prod.properties")) {
            assertNotNull(is, "application-prod.properties must exist");
            prodProps.load(is);
        }
        assertEquals("${DATABASE_URL}", prodProps.getProperty("spring.datasource.url"));
        assertEquals("${DATABASE_USERNAME}", prodProps.getProperty("spring.datasource.username"));
        assertEquals("${DATABASE_PASSWORD}", prodProps.getProperty("spring.datasource.password"));
        assertEquals("${JWT_SECRET}", prodProps.getProperty("nirvaha.jwt.secret"));
        assertEquals("${NIRVAHA_ALLOWED_ORIGINS}", prodProps.getProperty("nirvaha.allowed-origins"));
        assertEquals("true", prodProps.getProperty("spring.flyway.enabled"));
        assertEquals("${FLYWAY_BASELINE_ON_MIGRATE:false}", prodProps.getProperty("spring.flyway.baseline-on-migrate"));
        assertEquals("validate", prodProps.getProperty("spring.jpa.hibernate.ddl-auto"));
    }

    @Test
    void testFlywayBaselineMigrationScriptExists() {
        assertNotNull(getClass().getResourceAsStream("/db/migration/V1__initial_schema.sql"),
                "V1__initial_schema.sql baseline migration script must exist on classpath");
    }
}
