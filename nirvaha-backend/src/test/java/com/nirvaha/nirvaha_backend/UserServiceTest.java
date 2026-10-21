package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.exception.DuplicateResourceException;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.ResolutionRepository resolutionRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.IncidentActionRepository incidentActionRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.EvidenceRepository evidenceRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUserAndHashPassword() {
        CreateUserRequest request = new CreateUserRequest(
                "Puducherry Citizen",
                "citizen.service@example.com",
                "mySecretPassword",
                UserRole.CITIZEN
        );

        UserResponse response = userService.createUser(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Puducherry Citizen");
        assertThat(response.getEmail()).isEqualTo("citizen.service@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.CITIZEN);
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowExceptionOnDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "User One",
                "dup.service@example.com",
                "pass123",
                UserRole.CITIZEN
        );

        userService.createUser(request);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("dup.service@example.com");
    }

    @Test
    void shouldGetUserByIdSuccessfully() {
        CreateUserRequest request = new CreateUserRequest(
                "Officer Name",
                "officer@puducherry.gov.in",
                "officerPass123",
                UserRole.OFFICER
        );

        UserResponse created = userService.createUser(request);
        UserResponse fetched = userService.getUserById(created.getId());

        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getName()).isEqualTo("Officer Name");
        assertThat(fetched.getRole()).isEqualTo(UserRole.OFFICER);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        assertThatThrownBy(() -> userService.getUserById(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with ID 99999 not found");
    }
}
