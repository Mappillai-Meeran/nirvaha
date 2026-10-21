package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.RiskLevel;

import java.util.List;

public class WardRiskResponse {

    private Long wardId;
    private Integer wardNumber;
    private String wardName;
    private Double riskScore;     // Normalized (0.0 to 1.0)
    private Integer riskScore100; // Human-friendly (0 to 100)
    private RiskLevel riskLevel;
    private IncidentCategory topRiskCategory;
    private Integer activeIncidentCount;
    private Integer unresolvedIncidentCount;
    private List<RiskFactorContribution> factors;
    private String explanation;
    private String recommendedAction;
    private String weatherContext;

    public WardRiskResponse() {
    }

    public WardRiskResponse(Long wardId, Integer wardNumber, String wardName, Double riskScore,
                            Integer riskScore100, RiskLevel riskLevel, IncidentCategory topRiskCategory,
                            Integer activeIncidentCount, Integer unresolvedIncidentCount,
                            List<RiskFactorContribution> factors, String explanation,
                            String recommendedAction, String weatherContext) {
        this.wardId = wardId;
        this.wardNumber = wardNumber;
        this.wardName = wardName;
        this.riskScore = riskScore;
        this.riskScore100 = riskScore100;
        this.riskLevel = riskLevel;
        this.topRiskCategory = topRiskCategory;
        this.activeIncidentCount = activeIncidentCount;
        this.unresolvedIncidentCount = unresolvedIncidentCount;
        this.factors = factors;
        this.explanation = explanation;
        this.recommendedAction = recommendedAction;
        this.weatherContext = weatherContext;
    }

    public Long getWardId() {
        return wardId;
    }

    public void setWardId(Long wardId) {
        this.wardId = wardId;
    }

    public Integer getWardNumber() {
        return wardNumber;
    }

    public void setWardNumber(Integer wardNumber) {
        this.wardNumber = wardNumber;
    }

    public String getWardName() {
        return wardName;
    }

    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public Integer getRiskScore100() {
        return riskScore100;
    }

    public void setRiskScore100(Integer riskScore100) {
        this.riskScore100 = riskScore100;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public IncidentCategory getTopRiskCategory() {
        return topRiskCategory;
    }

    public void setTopRiskCategory(IncidentCategory topRiskCategory) {
        this.topRiskCategory = topRiskCategory;
    }

    public Integer getActiveIncidentCount() {
        return activeIncidentCount;
    }

    public void setActiveIncidentCount(Integer activeIncidentCount) {
        this.activeIncidentCount = activeIncidentCount;
    }

    public Integer getUnresolvedIncidentCount() {
        return unresolvedIncidentCount;
    }

    public void setUnresolvedIncidentCount(Integer unresolvedIncidentCount) {
        this.unresolvedIncidentCount = unresolvedIncidentCount;
    }

    public List<RiskFactorContribution> getFactors() {
        return factors;
    }

    public void setFactors(List<RiskFactorContribution> factors) {
        this.factors = factors;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getWeatherContext() {
        return weatherContext;
    }

    public void setWeatherContext(String weatherContext) {
        this.weatherContext = weatherContext;
    }
}
