package com.nirvaha.nirvaha_backend.dto;

public class RiskFactorContribution {

    private String name;
    private Double value;        // Normalized factor value (0.0 to 1.0)
    private Double contribution; // Weighted contribution to final score
    private String description;

    public RiskFactorContribution() {
    }

    public RiskFactorContribution(String name, Double value, Double contribution, String description) {
        this.name = name;
        this.value = value;
        this.contribution = contribution;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getContribution() {
        return contribution;
    }

    public void setContribution(Double contribution) {
        this.contribution = contribution;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
