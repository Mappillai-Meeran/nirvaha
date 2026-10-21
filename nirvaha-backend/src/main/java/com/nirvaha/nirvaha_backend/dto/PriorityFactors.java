package com.nirvaha.nirvaha_backend.dto;

public class PriorityFactors {

    private double severityFactor;
    private double reportVolumeFactor;
    private double locationImpactFactor;
    private double rainfallFactor;

    public PriorityFactors() {
    }

    public PriorityFactors(double severityFactor, double reportVolumeFactor, double locationImpactFactor, double rainfallFactor) {
        this.severityFactor = severityFactor;
        this.reportVolumeFactor = reportVolumeFactor;
        this.locationImpactFactor = locationImpactFactor;
        this.rainfallFactor = rainfallFactor;
    }

    public double getSeverityFactor() {
        return severityFactor;
    }

    public void setSeverityFactor(double severityFactor) {
        this.severityFactor = severityFactor;
    }

    public double getReportVolumeFactor() {
        return reportVolumeFactor;
    }

    public void setReportVolumeFactor(double reportVolumeFactor) {
        this.reportVolumeFactor = reportVolumeFactor;
    }

    public double getLocationImpactFactor() {
        return locationImpactFactor;
    }

    public void setLocationImpactFactor(double locationImpactFactor) {
        this.locationImpactFactor = locationImpactFactor;
    }

    public double getRainfallFactor() {
        return rainfallFactor;
    }

    public void setRainfallFactor(double rainfallFactor) {
        this.rainfallFactor = rainfallFactor;
    }
}
