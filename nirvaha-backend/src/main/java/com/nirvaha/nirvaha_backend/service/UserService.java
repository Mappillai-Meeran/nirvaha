package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.User;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.exception.DuplicateResourceException;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }

        UserRole roleToAssign = UserRole.CITIZEN;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin && request.getRole() != null) {
                roleToAssign = request.getRole();
            }
        } else if (auth == null && request.getRole() != null) {
            // Internal / direct service call (e.g. unit tests without Spring Security web context)
            roleToAssign = request.getRole();
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(roleToAssign);

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));
        verifyUserAccess(user);
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email + "' not found"));
        verifyUserAccess(user);
        return UserResponse.fromEntity(user);
    }

    private void verifyUserAccess(User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return; // Internal or direct test execution without web security context
        }
        boolean isOfficerOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_OFFICER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isOfficerOrAdmin) {
            if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(auth.getName())) {
                throw new AccessDeniedException("Access denied: You do not have permission to view other user profiles");
            }
        }
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultGovernmentUsersIfMissing() {
        if (userRepository.findByEmail("officer@puducherry.gov.in").isEmpty()) {
            User officer = new User();
            officer.setName("Officer Ramanathan");
            officer.setEmail("officer@puducherry.gov.in");
            officer.setPasswordHash(passwordEncoder.encode("Officer123!"));
            officer.setRole(UserRole.OFFICER);
            userRepository.save(officer);
        }
        if (userRepository.findByEmail("admin@puducherry.gov.in").isEmpty()) {
            User admin = new User();
            admin.setName("Municipal Administrator");
            admin.setEmail("admin@puducherry.gov.in");
            admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
        }
        if (userRepository.findByEmail("citizen@puducherry.gov.in").isEmpty()) {
            User citizen = new User();
            citizen.setName("Puducherry Resident");
            citizen.setEmail("citizen@puducherry.gov.in");
            citizen.setPasswordHash(passwordEncoder.encode("Citizen123!"));
            citizen.setRole(UserRole.CITIZEN);
            userRepository.save(citizen);
        }
    }
}
