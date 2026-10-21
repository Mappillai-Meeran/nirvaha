package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.DataSource;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.WeatherObservation;
import com.nirvaha.nirvaha_backend.repository.DataSourceRepository;
import com.nirvaha.nirvaha_backend.repository.WeatherObservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class ImdWeatherAdapter {

    private static final Logger log = LoggerFactory.getLogger(ImdWeatherAdapter.class);

    private final WeatherObservationRepository weatherRepository;
    private final DataSourceRepository dataSourceRepository;
    private final String imdApiKey;
    private final String imdApiUrl;

    public ImdWeatherAdapter(WeatherObservationRepository weatherRepository,
                             DataSourceRepository dataSourceRepository,
                             @Value("${nirvaha.imd.api-key:}") String imdApiKey,
                             @Value("${nirvaha.imd.api-url:https://api.imd.gov.in/api/v1}") String imdApiUrl) {
        this.weatherRepository = weatherRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.imdApiKey = imdApiKey != null ? imdApiKey.trim() : "";
        this.imdApiUrl = imdApiUrl;
    }

    private volatile boolean authorized = false;

    public boolean isConfigured() {
        return !imdApiKey.isBlank();
    }

    public boolean isAuthorized() {
        return isConfigured() && authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public String getImdStatus() {
        if (!isConfigured()) {
            return "NOT_CONFIGURED";
        }
        if (!isAuthorized()) {
            return "PENDING_AUTHORIZATION";
        }
        return "CONNECTED";
    }

    public String getEndpoint() {
        return imdApiUrl + "/cityforecast";
    }

    @Transactional
    public WeatherObservation recordObservationIfMissing(String provider, String stationCode, String obsType,
                                                         LocalDateTime observedAt, LocalDateTime periodStart,
                                                         LocalDateTime periodEnd, Double rainfallMm, Double normalMm,
                                                         Double departurePct, String category, DataSource source,
                                                         String sourceRecordId) {
        Optional<WeatherObservation> existing = weatherRepository
                .findObservation(provider, stationCode, obsType, observedAt);
        if (existing.isPresent()) {
            return existing.get();
        }

        String payloadStr = String.format("%s:%s:%s:%s:%.2f", provider, stationCode, obsType, observedAt, rainfallMm);
        String hash = computeSha256(payloadStr);

        Optional<WeatherObservation> hashMatch = weatherRepository.findByRawPayloadHash(hash);
        if (hashMatch.isPresent()) {
            return hashMatch.get();
        }

        WeatherObservation obs = new WeatherObservation();
        obs.setProvider(provider);
        obs.setStationOrDistrictCode(stationCode);
        obs.setObservationType(obsType);
        obs.setObservedAt(observedAt);
        obs.setPeriodStart(periodStart);
        obs.setPeriodEnd(periodEnd);
        obs.setRainfallMm(rainfallMm);
        obs.setNormalRainfallMm(normalMm);
        obs.setDeparturePercent(departurePct);
        obs.setRainfallCategory(category);
        obs.setRawPayloadHash(hash);
        obs.setSource(source);
        obs.setRetrievedAt(LocalDateTime.now());
        obs.setSourceRecordId(sourceRecordId);
        obs.setDataVersion("1.0");
        obs.setProvenanceStatus("VERIFIED_OFFICIAL_IMD");

        return weatherRepository.save(obs);
    }

    public String generateRainfallContextForIncident(Incident incident) {
        if (incident == null || incident.getCreatedAt() == null) {
            return "No rainfall context available.";
        }
        // Drainage or flooding relevance check
        if (incident.getCategory() != IncidentCategory.DRAINAGE &&
            incident.getCategory() != IncidentCategory.WATER_SUPPLY &&
            incident.getCategory() != IncidentCategory.POTHOLE) {
            return "Weather context: Incident category " + incident.getCategory() + " has standard environmental relevance.";
        }

        LocalDateTime incidentTime = incident.getCreatedAt();
        LocalDateTime windowStart = incidentTime.minusHours(48);
        List<WeatherObservation> observations = weatherRepository.findObservationsInWindow(windowStart, incidentTime.plusHours(6));

        if (observations.isEmpty()) {
            return "No official IMD observations recorded in the 48-hour temporal window preceding this incident. Live risk evaluated using real-time atmospheric readings and municipal history.";
        }

        WeatherObservation highestObs = observations.stream()
                .max((a, b) -> Double.compare(a.getRainfallMm() != null ? a.getRainfallMm() : 0.0,
                        b.getRainfallMm() != null ? b.getRainfallMm() : 0.0))
                .orElse(observations.get(0));

        double rainfall = highestObs.getRainfallMm() != null ? highestObs.getRainfallMm() : 0.0;
        return String.format("[OFFICIAL WEATHER - IMD] Observed %.1f mm of rainfall (Category: %s) within the temporal window preceding this incident.",
                rainfall, highestObs.getRainfallCategory() != null ? highestObs.getRainfallCategory() : "NORMAL");
    }

    public List<WeatherObservation> getRecentObservations() {
        return weatherRepository.findRecentObservations();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
