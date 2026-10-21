package com.nirvaha.nirvaha_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ActionCenterQueueDTO {

    private List<ActionRecommendationDTO> recommendations;
    private List<LearningSignalDTO> learningSignals;
    private Integer totalActionableCount;
    private Integer criticalCount;
    private LocalDateTime evaluatedAt;

    public ActionCenterQueueDTO() {
    }

    public ActionCenterQueueDTO(List<ActionRecommendationDTO> recommendations, List<LearningSignalDTO> learningSignals,
                                Integer totalActionableCount, Integer criticalCount, LocalDateTime evaluatedAt) {
        this.recommendations = recommendations;
        this.learningSignals = learningSignals;
        this.totalActionableCount = totalActionableCount;
        this.criticalCount = criticalCount;
        this.evaluatedAt = evaluatedAt;
    }

    public List<ActionRecommendationDTO> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<ActionRecommendationDTO> recommendations) {
        this.recommendations = recommendations;
    }

    public List<LearningSignalDTO> getLearningSignals() {
        return learningSignals;
    }

    public void setLearningSignals(List<LearningSignalDTO> learningSignals) {
        this.learningSignals = learningSignals;
    }

    public Integer getTotalActionableCount() {
        return totalActionableCount;
    }

    public void setTotalActionableCount(Integer totalActionableCount) {
        this.totalActionableCount = totalActionableCount;
    }

    public Integer getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(Integer criticalCount) {
        this.criticalCount = criticalCount;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
