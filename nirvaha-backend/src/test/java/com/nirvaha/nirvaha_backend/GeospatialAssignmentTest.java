package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.service.AdministrativeAreaService;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.dto.CreateIncidentRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GeospatialAssignmentTest {

    @Autowired
    private AdministrativeAreaService administrativeAreaService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.AdministrativeAreaRepository areaRepository;

    private User citizenUser;

    @BeforeEach
    void setUp() {
        citizenUser = new User();
        citizenUser.setEmail("citizen.geo@example.com");
        citizenUser.setName("Citizen Geo Test");
        citizenUser.setRole(UserRole.CITIZEN);
        citizenUser.setPasswordHash("hash123");

        areaRepository.findByOfficialCode("PY-WARD-012").ifPresent(ward -> {
            ward.setValidationStatus(ValidationStatus.VERIFIED);
            areaRepository.save(ward);
        });
    }

    @Test
    void testPointInsideVerifiedPolygonAssignment() {
        // Incident inside Oulgaret Ward 12 polygon (Lat: 11.940, Lng: 79.800)
        Incident incident = new Incident();
        incident.setLatitude(BigDecimal.valueOf(11.9400000));
        incident.setLongitude(BigDecimal.valueOf(79.8000000));

        administrativeAreaService.assignIncidentToWard(incident);

        assertThat(incident.getAssignedArea()).isNotNull();
        assertThat(incident.getAssignedArea().getOfficialCode()).isEqualTo("PY-WARD-012");
        assertThat(incident.getAssignmentMethod()).isEqualTo(AssignmentMethod.POINT_IN_POLYGON);
        assertThat(incident.getAssignmentConfidence()).isEqualTo(BigDecimal.valueOf(1.0000));
    }

    @Test
    void testPointOutsideVerifiedPolygonRemainsUnassigned() {
        // Point far outside all Puducherry ward polygons (Lat: 13.0827, Lng: 80.2707 - Chennai)
        Incident incident = new Incident();
        incident.setLatitude(BigDecimal.valueOf(13.0827000));
        incident.setLongitude(BigDecimal.valueOf(80.2707000));

        administrativeAreaService.assignIncidentToWard(incident);

        assertThat(incident.getAssignedArea()).isNull();
        assertThat(incident.getAssignmentMethod()).isEqualTo(AssignmentMethod.UNASSIGNED);
        assertThat(incident.getWardAssignmentNotes()).contains("WARD ASSIGNMENT REVIEW REQUIRED");
    }
}
