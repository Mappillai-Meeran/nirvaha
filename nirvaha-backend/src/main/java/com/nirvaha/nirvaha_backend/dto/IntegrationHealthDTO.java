package com.nirvaha.nirvaha_backend.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IntegrationHealthDTO {

    private String systemStatus;
    private LocalDateTime evaluatedAt;
    private List<IntegrationDetailDTO> integrations = new ArrayList<>();

    public IntegrationHealthDTO() {
    }

    public IntegrationHealthDTO(String systemStatus, LocalDateTime evaluatedAt, List<IntegrationDetailDTO> integrations) {
        this.systemStatus = systemStatus;
        this.evaluatedAt = evaluatedAt;
        this.integrations = integrations != null ? integrations : new ArrayList<>();
    }

    public String getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(String systemStatus) {
        this.systemStatus = systemStatus;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public List<IntegrationDetailDTO> getIntegrations() {
        return integrations;
    }

    public void setIntegrations(List<IntegrationDetailDTO> integrations) {
        this.integrations = integrations;
    }

    public static class IntegrationDetailDTO {
        private String name;
        private String category;
        private String status;
        private String endpoint;
        private String authType;
        private LocalDateTime lastObservationTimestamp;
        private LocalDateTime lastRetrievalTimestamp;
        private String observationSummary;
        private String errorCategory;

        public IntegrationDetailDTO() {
        }

        public IntegrationDetailDTO(String name, String category, String status, String endpoint,
                                    String authType, LocalDateTime lastObservationTimestamp,
                                    LocalDateTime lastRetrievalTimestamp, String observationSummary,
                                    String errorCategory) {
            this.name = name;
            this.category = category;
            this.status = status;
            this.endpoint = endpoint;
            this.authType = authType;
            this.lastObservationTimestamp = lastObservationTimestamp;
            this.lastRetrievalTimestamp = lastRetrievalTimestamp;
            this.observationSummary = observationSummary;
            this.errorCategory = errorCategory;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public LocalDateTime getLastObservationTimestamp() {
            return lastObservationTimestamp;
        }

        public void setLastObservationTimestamp(LocalDateTime lastObservationTimestamp) {
            this.lastObservationTimestamp = lastObservationTimestamp;
        }

        public LocalDateTime getLastRetrievalTimestamp() {
            return lastRetrievalTimestamp;
        }

        public void setLastRetrievalTimestamp(LocalDateTime lastRetrievalTimestamp) {
            this.lastRetrievalTimestamp = lastRetrievalTimestamp;
        }

        public String getObservationSummary() {
            return observationSummary;
        }

        public void setObservationSummary(String observationSummary) {
            this.observationSummary = observationSummary;
        }

        public String getErrorCategory() {
            return errorCategory;
        }

        public void setErrorCategory(String errorCategory) {
            this.errorCategory = errorCategory;
        }
    }
}
