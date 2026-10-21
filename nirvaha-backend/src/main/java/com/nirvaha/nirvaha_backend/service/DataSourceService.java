package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.DataSource;
import com.nirvaha.nirvaha_backend.entity.FreshnessStatus;
import com.nirvaha.nirvaha_backend.entity.SourceType;
import com.nirvaha.nirvaha_backend.repository.DataSourceRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;

    public DataSourceService(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedInitialDataSources() {
        seedOrUpdateSource("Puducherry Local Administration Department (LAD)",
                "Puducherry LAD",
                SourceType.OFFICIAL_GIS,
                "https://lad.py.gov.in/maps",
                "Puducherry Ward Delimitation & Maps Catalog",
                "Official Public Domain / Govt. of Puducherry",
                "OFFICIAL_GAZETTE_EXTRACTION",
                "MONTHLY",
                "Puducherry municipal and commune ward boundaries extracted from official LAD delimitation notifications. Approximated bounding envelopes.",
                "Puducherry UT",
                "ACTIVE",
                LocalDateTime.now().minusDays(1));

        seedOrUpdateSource("India Open Government Data Platform (OGD)",
                "National Informatics Centre (NIC) / OGD India",
                SourceType.OGD_DATASET,
                "https://data.gov.in/catalog/puducherry-civic-infrastructure",
                "Puducherry Civic Infrastructure Datasets",
                "Open Government Data License - India",
                "API_PULL",
                "QUARTERLY",
                "Catalog entry registered. Programmatic ingestion endpoint requires registered developer API key token on api.data.gov.in.",
                "Puducherry UT",
                "NOT_CONFIGURED",
                null);

        seedOrUpdateSource("India Meteorological Department (IMD)",
                "Ministry of Earth Sciences / IMD",
                SourceType.OFFICIAL_WEATHER,
                "https://mausam.imd.gov.in/",
                "Puducherry District Daily Rainfall & Weather Observations",
                "Government of India Public Data",
                "AUTOMATED_API_ADAPTER",
                "DAILY",
                "Official historical daily rainfall measurements for Puducherry District. Upstream REST API adapter awaiting institutional X-API-KEY and JWT credentials.",
                "Puducherry District",
                "NOT_CONFIGURED",
                null);

        seedOrUpdateSource("Puducherry Public Works Department (PWD)",
                "Government of Puducherry PWD",
                SourceType.OFFICIAL_POLICY,
                "https://pwd.py.gov.in/citizens-charter",
                "PWD Citizen's Charter 2024",
                "Government Policy Document",
                "MANUAL_POLICY_REGISTRATION",
                "YEARLY",
                "Official service standards and resolution targets including urban drainage maintenance (3-day target) and road potholes (15-day target).",
                "Puducherry UT",
                "ACTIVE",
                LocalDateTime.now().minusDays(30));

        seedOrUpdateSource("OpenStreetMap Contributors",
                "OpenStreetMap Foundation",
                SourceType.OPEN_GEOGRAPHIC,
                "https://www.openstreetmap.org",
                "Puducherry Basemap & Road Network",
                "Open Database License (ODbL) 1.0 - © OpenStreetMap contributors",
                "TILE_SERVER",
                "CONTINUOUS",
                "Geographic context and street basemaps for visual mapping via raster tiles. Overpass live feature queries disabled.",
                "Global / Puducherry",
                "ACTIVE",
                LocalDateTime.now().minusHours(1));

        seedOrUpdateSource("Nirvaha Citizen Evidence Network",
                "Nirvaha Civic Intelligence Platform",
                SourceType.NIRVAHA_CITIZEN,
                "https://nirvaha.gov.in",
                "Nirvaha Citizen-Reported Incidents & Verification Evidence",
                "Nirvaha Platform Terms of Service",
                "CITIZEN_SUBMISSION",
                "REALTIME",
                "Verified citizen-reported civic complaints, geotagged photos, and resolution evidence.",
                "Puducherry UT",
                "ACTIVE",
                LocalDateTime.now());

        seedOrUpdateSource("Open-Meteo Real-Time Weather & Forecast API",
                "Open-Meteo / ECMWF",
                SourceType.OFFICIAL_WEATHER,
                "https://api.open-meteo.com/v1/forecast",
                "Puducherry High-Resolution Weather & Precipitation Forecast",
                "Open Database License (ODbL) / CC BY 4.0",
                "LIVE_REST_API",
                "HOURLY",
                "Live real-time atmospheric measurements (temperature, humidity, precipitation) and 24-hour deterministic rain forecasts for Puducherry.",
                "Puducherry UT",
                "ACTIVE",
                LocalDateTime.now());

        seedOrUpdateSource("Pondicherry Municipality Grievance Redressal",
                "Pondicherry Municipality, Govt. of Puducherry",
                SourceType.OFFICIAL_POLICY,
                "https://pdymun.in/grievance-redressal.php",
                "Pondicherry Municipal Citizen Charter & Engineering Wing Service Standards",
                "Govt. of Puducherry Official Policy",
                "CHARTER_POLICY_INTEGRATION",
                "DAILY",
                "Official citizen charter turnaround standards referenced offline. Programmatic intake disabled; requires NIC bridge.",
                "Puducherry Municipality",
                "POLICY_REFERENCE",
                null);

        seedOrUpdateSource("Puducherry LG's Grievance Redressal System",
                "Lieutenant Governor's Secretariat, Puducherry",
                SourceType.OFFICIAL_POLICY,
                "https://lgredressal.py.gov.in/pgrs/",
                "LG Public Grievance Redressal Desk",
                "Govt. of Puducherry Official Policy",
                "RESTRICTED_DEPARTMENTAL_ACCESS",
                "REALTIME",
                "State apex citizen grievance monitoring portal. Programmatic integration disabled; requires departmental MOU.",
                "Puducherry UT",
                "RESTRICTED_ACCESS",
                null);
    }

    private void seedOrUpdateSource(String name, String provider, SourceType sourceType, String sourceUrl,
                                     String datasetName, String license, String retrievalMethod,
                                     String updateFrequency, String notes, String coverage,
                                     String status, LocalDateTime retrievedAt) {
        Optional<DataSource> existingOpt = dataSourceRepository.findByName(name);
        DataSource ds = existingOpt.orElseGet(DataSource::new);
        ds.setName(name);
        ds.setProvider(provider);
        ds.setSourceType(sourceType);
        ds.setSourceUrl(sourceUrl);
        ds.setDatasetName(datasetName);
        ds.setLicense(license);
        ds.setRetrievalMethod(retrievalMethod);
        ds.setUpdateFrequency(updateFrequency);
        ds.setRetrievedAt(retrievedAt);
        if (ds.getPublishedAt() == null) {
            ds.setPublishedAt(LocalDateTime.now().minusDays(30));
        }
        ds.setVersion("1.0.0");
        ds.setCoverage(coverage);
        ds.setStatus(status);
        ds.setNotes(notes);
        dataSourceRepository.save(ds);
    }


    public List<DataSource> getAllDataSources() {
        return dataSourceRepository.findAll();
    }

    public Optional<DataSource> getDataSourceById(Long id) {
        return dataSourceRepository.findById(id);
    }

    public Optional<DataSource> getDataSourceByName(String name) {
        return dataSourceRepository.findByName(name);
    }

    public FreshnessStatus calculateFreshness(DataSource source) {
        if (source == null || source.getRetrievedAt() == null || source.getUpdateFrequency() == null) {
            return FreshnessStatus.UNKNOWN;
        }
        long daysSinceRetrieval = Duration.between(source.getRetrievedAt(), LocalDateTime.now()).toDays();
        String freq = source.getUpdateFrequency().toUpperCase();
        if (freq.contains("DAILY") || freq.contains("REALTIME")) {
            return daysSinceRetrieval <= 2 ? FreshnessStatus.CURRENT : (daysSinceRetrieval <= 7 ? FreshnessStatus.STALE : FreshnessStatus.OUTDATED);
        } else if (freq.contains("MONTHLY")) {
            return daysSinceRetrieval <= 35 ? FreshnessStatus.CURRENT : FreshnessStatus.STALE;
        } else if (freq.contains("QUARTERLY") || freq.contains("YEARLY")) {
            return daysSinceRetrieval <= 180 ? FreshnessStatus.CURRENT : FreshnessStatus.STALE;
        }
        return FreshnessStatus.CURRENT;
    }
}
