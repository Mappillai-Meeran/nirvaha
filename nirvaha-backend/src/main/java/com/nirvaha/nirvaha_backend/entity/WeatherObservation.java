package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_observations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "station_or_district_code", "observation_type", "observed_at"})
})
public class WeatherObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(name = "station_or_district_code", nullable = false, length = 100)
    private String stationOrDistrictCode;

    @Column(name = "observation_type", nullable = false, length = 100)
    private String observationType;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "rainfall_mm")
    private Double rainfallMm;

    @Column(name = "normal_rainfall_mm")
    private Double normalRainfallMm;

    @Column(name = "departure_percent")
    private Double departurePercent;

    @Column(name = "rainfall_category", length = 50)
    private String rainfallCategory;

    @Column(name = "raw_payload_hash", length = 128)
    private String rawPayloadHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private DataSource source;

    @Column(name = "retrieved_at", nullable = false)
    private LocalDateTime retrievedAt;

    @Column(name = "source_record_id", length = 100)
    private String sourceRecordId;

    @Column(name = "data_version", length = 50)
    private String dataVersion;

    @Column(name = "provenance_status", length = 50)
    private String provenanceStatus;

    public WeatherObservation() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.retrievedAt == null) {
            this.retrievedAt = LocalDateTime.now();
        }
        if (this.provenanceStatus == null) {
            this.provenanceStatus = "VERIFIED_OFFICIAL";
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStationOrDistrictCode() {
        return stationOrDistrictCode;
    }

    public void setStationOrDistrictCode(String stationOrDistrictCode) {
        this.stationOrDistrictCode = stationOrDistrictCode;
    }

    public String getObservationType() {
        return observationType;
    }

    public void setObservationType(String observationType) {
        this.observationType = observationType;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Double getRainfallMm() {
        return rainfallMm;
    }

    public void setRainfallMm(Double rainfallMm) {
        this.rainfallMm = rainfallMm;
    }

    public Double getNormalRainfallMm() {
        return normalRainfallMm;
    }

    public void setNormalRainfallMm(Double normalRainfallMm) {
        this.normalRainfallMm = normalRainfallMm;
    }

    public Double getDeparturePercent() {
        return departurePercent;
    }

    public void setDeparturePercent(Double departurePercent) {
        this.departurePercent = departurePercent;
    }

    public String getRainfallCategory() {
        return rainfallCategory;
    }

    public void setRainfallCategory(String rainfallCategory) {
        this.rainfallCategory = rainfallCategory;
    }

    public String getRawPayloadHash() {
        return rawPayloadHash;
    }

    public void setRawPayloadHash(String rawPayloadHash) {
        this.rawPayloadHash = rawPayloadHash;
    }

    public DataSource getSource() {
        return source;
    }

    public void setSource(DataSource source) {
        this.source = source;
    }

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public String getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(String sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    public String getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(String dataVersion) {
        this.dataVersion = dataVersion;
    }

    public String getProvenanceStatus() {
        return provenanceStatus;
    }

    public void setProvenanceStatus(String provenanceStatus) {
        this.provenanceStatus = provenanceStatus;
    }
}
