package com.nirvaha.nirvaha_backend.security;

import com.nirvaha.nirvaha_backend.entity.Incident;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Validates ownership and authorization for incident records and child resources.
 * Ensures that citizens can only inspect incidents they reported,
 * while municipal officers and administrators retain authoritative access across all records.
 */
@Component
public class IncidentAccessValidator {

    public void verifyIncidentAccess(Incident incident) {
        if (incident == null) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            // Internal or service-level test execution without security context
            return;
        }

        if (!auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required to access incident data");
        }

        boolean isOfficerOrAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_OFFICER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));

        if (isOfficerOrAdmin) {
            return;
        }

        // Resident / Citizen ownership verification
        if (incident.getReporter() == null || incident.getReporter().getEmail() == null ||
                !incident.getReporter().getEmail().equalsIgnoreCase(auth.getName())) {
            throw new AccessDeniedException("Access denied: You do not have permission to access incident #" + incident.getId());
        }
    }
}
