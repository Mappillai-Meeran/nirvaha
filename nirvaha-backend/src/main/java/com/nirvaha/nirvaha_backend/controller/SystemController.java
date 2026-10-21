package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.IntegrationHealthDTO;
import com.nirvaha.nirvaha_backend.service.IntegrationHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final IntegrationHealthService integrationHealthService;

    public SystemController(IntegrationHealthService integrationHealthService) {
        this.integrationHealthService = integrationHealthService;
    }

    @GetMapping("/integration-health")
    public ResponseEntity<IntegrationHealthDTO> getIntegrationHealth() {
        return ResponseEntity.ok(integrationHealthService.evaluateSystemIntegrations());
    }
}
