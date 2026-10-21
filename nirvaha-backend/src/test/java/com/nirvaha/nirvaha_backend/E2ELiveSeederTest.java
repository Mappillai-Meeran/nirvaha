package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class E2ELiveSeederTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void seedOfficerAndAdminForE2EVerification() {
        String officerEmail = "officer.e2e@puducherry.gov.in";
        if (userRepository.findByEmail(officerEmail).isEmpty()) {
            userService.createUser(new CreateUserRequest("E2E Puducherry Officer", officerEmail, "Password123!", UserRole.OFFICER));
        }

        String adminEmail = "admin.e2e@puducherry.gov.in";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            userService.createUser(new CreateUserRequest("E2E Puducherry Admin", adminEmail, "Password123!", UserRole.ADMIN));
        }
    }
}
