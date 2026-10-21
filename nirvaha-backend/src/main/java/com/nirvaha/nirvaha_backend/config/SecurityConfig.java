package com.nirvaha.nirvaha_backend.config;

import com.nirvaha.nirvaha_backend.security.JwtAuthenticationFilter;
import com.nirvaha.nirvaha_backend.security.RateLimitingFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          RateLimitingFilter rateLimitingFilter,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .contentTypeOptions(config -> {})
                .frameOptions(config -> config.deny())
                .referrerPolicy(config -> config.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required to access this resource\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access Denied: You do not have sufficient privileges to access this resource\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public Endpoints & Health Check
                .requestMatchers("/actuator/health", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password", "/api/language/**", "/api/system/**", "/api/provenance/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                // Authenticated Profile Management & In-App Notifications
                .requestMatchers("/api/auth/me", "/api/auth/change-password").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")
                .requestMatchers("/api/notifications/**").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")

                // Incident Creation & Resident-Owned Reports
                .requestMatchers(HttpMethod.POST, "/api/incidents").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/incidents/my-reports").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")

                // General Incident Listing: Officers & Admins Only (Strict Resident Privacy Isolation)
                .requestMatchers(HttpMethod.GET, "/api/incidents").hasAnyRole("OFFICER", "ADMIN")

                // Specific Incident & Sub-resource Inspection (Subject to Service-Level Ownership Verification)
                .requestMatchers(HttpMethod.GET, "/api/incidents/*").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/incidents/*/**").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")

                // Resident Resolution Feedback (Confirm / Dispute): Citizen Only
                .requestMatchers(HttpMethod.POST, "/api/incidents/*/feedback").hasRole("CITIZEN")
                .requestMatchers(HttpMethod.POST, "/api/incidents/*/feedback/*/review").hasAnyRole("OFFICER", "ADMIN")

                // Government Workflow Mutations: Officer & Admin Only
                .requestMatchers(HttpMethod.POST, "/api/incidents/*/actions", "/api/incidents/*/resolutions").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/evidence/**").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")
                .requestMatchers("/api/duplicates/**").hasAnyRole("CITIZEN", "OFFICER", "ADMIN")


                // Government Operational Endpoints: Officer & Admin Only
                .requestMatchers("/api/priority/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/workflow/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/resolutions/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/integrity/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/risk/wards/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/intelligence/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/action-center/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/demo/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/sla/**").hasAnyRole("OFFICER", "ADMIN")

                // Admin Re-evaluation Operations: Admin Only
                .requestMatchers(HttpMethod.POST, "/api/risk/assessments/evaluate").hasRole("ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${nirvaha.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        java.util.List<String> origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
