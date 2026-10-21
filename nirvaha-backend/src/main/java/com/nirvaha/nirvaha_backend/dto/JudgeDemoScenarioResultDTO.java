package com.nirvaha.nirvaha_backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class JudgeDemoScenarioResultDTO {

    private String demoRunId;
    private String scenarioKey;
    private String scenarioTitle;
    private String status;
    private String executionMode; // "BACKEND_LIVE"
    private String dataClassification; // "SIMULATED"
    private String initialSummary;
    private String actionSummary;
    private String outcomeSummary;
    private Long targetIncidentId;
    private List<Long> affectedIncidentIds;
    private Map<String, Object> metrics;
    private LocalDateTime executedAt;

    public JudgeDemoScenarioResultDTO() {
    }

    public JudgeDemoScenarioResultDTO(String scenarioKey, String scenarioTitle, String status,
                                      String executionMode, String dataClassification,
                                      String initialSummary, String actionSummary, String outcomeSummary,
                                      Long targetIncidentId, List<Long> affectedIncidentIds,
                                      Map<String, Object> metrics, LocalDateTime executedAt) {
        this(null, scenarioKey, scenarioTitle, status, executionMode, dataClassification,
                initialSummary, actionSummary, outcomeSummary, targetIncidentId, affectedIncidentIds, metrics, executedAt);
    }

    public JudgeDemoScenarioResultDTO(String demoRunId, String scenarioKey, String scenarioTitle, String status,
                                      String executionMode, String dataClassification,
                                      String initialSummary, String actionSummary, String outcomeSummary,
                                      Long targetIncidentId, List<Long> affectedIncidentIds,
                                      Map<String, Object> metrics, LocalDateTime executedAt) {
        this.demoRunId = demoRunId;
        this.scenarioKey = scenarioKey;
        this.scenarioTitle = scenarioTitle;
        this.status = status;
        this.executionMode = executionMode;
        this.dataClassification = dataClassification;
        this.initialSummary = initialSummary;
        this.actionSummary = actionSummary;
        this.outcomeSummary = outcomeSummary;
        this.targetIncidentId = targetIncidentId;
        this.affectedIncidentIds = affectedIncidentIds;
        this.metrics = metrics;
        this.executedAt = executedAt;
    }

    public String getDemoRunId() {
        return demoRunId;
    }

    public void setDemoRunId(String demoRunId) {
        this.demoRunId = demoRunId;
    }

    public String getScenarioKey() {
        return scenarioKey;
    }

    public void setScenarioKey(String scenarioKey) {
        this.scenarioKey = scenarioKey;
    }

    public String getScenarioTitle() {
        return scenarioTitle;
    }

    public void setScenarioTitle(String scenarioTitle) {
        this.scenarioTitle = scenarioTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getDataClassification() {
        return dataClassification;
    }

    public void setDataClassification(String dataClassification) {
        this.dataClassification = dataClassification;
    }

    public String getInitialSummary() {
        return initialSummary;
    }

    public void setInitialSummary(String initialSummary) {
        this.initialSummary = initialSummary;
    }

    public String getActionSummary() {
        return actionSummary;
    }

    public void setActionSummary(String actionSummary) {
        this.actionSummary = actionSummary;
    }

    public String getOutcomeSummary() {
        return outcomeSummary;
    }

    public void setOutcomeSummary(String outcomeSummary) {
        this.outcomeSummary = outcomeSummary;
    }

    public Long getTargetIncidentId() {
        return targetIncidentId;
    }

    public void setTargetIncidentId(Long targetIncidentId) {
        this.targetIncidentId = targetIncidentId;
    }

    public List<Long> getAffectedIncidentIds() {
        return affectedIncidentIds;
    }

    public void setAffectedIncidentIds(List<Long> affectedIncidentIds) {
        this.affectedIncidentIds = affectedIncidentIds;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
