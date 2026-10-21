package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.UserController;
import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.exception.DuplicateResourceException;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateUserSuccessfully() throws Exception {
        UserResponse response = new UserResponse(1L, "Puducherry Citizen", "citizen.pdu@example.com", UserRole.CITIZEN, LocalDateTime.now());
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "name": "Puducherry Citizen",
                    "email": "citizen.pdu@example.com",
                    "password": "securePassword123",
                    "role": "CITIZEN"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Puducherry Citizen"))
                .andExpect(jsonPath("$.email").value("citizen.pdu@example.com"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    void shouldReturnConflictOnDuplicateEmail() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateResourceException("User with email 'dup@example.com' already exists"));

        String jsonPayload = """
                {
                    "name": "User Duplicate",
                    "email": "dup@example.com",
                    "password": "securePassword123",
                    "role": "CITIZEN"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User with email 'dup@example.com' already exists"));
    }

    @Test
    void shouldGetUserByIdSuccessfully() throws Exception {
        UserResponse response = new UserResponse(1L, "Officer Test", "officer@example.com", UserRole.OFFICER, LocalDateTime.now());
        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Officer Test"))
                .andExpect(jsonPath("$.role").value("OFFICER"));
    }

    @Test
    void shouldReturn404ForNonExistentUser() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResourceNotFoundException("User with ID 99 not found"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with ID 99 not found"));
    }
}
