package com.nirvaha.nirvaha_backend.entity;

/**
 * Distinguishes the intake provenance of civic incident records in Nirvaha.
 */
public enum IncidentSource {
    /**
     * A genuine civic report submitted by an authenticated or verified resident.
     */
    RESIDENT_SUBMITTED,

    /**
     * A verified record ingested via an authorized government integration or institutional portal.
     */
    AUTHORIZED_GOVERNMENT_SOURCE,

    /**
     * An environmental observation, meteorological warning, or sensor dataset from an official provider.
     */
    OFFICIAL_EXTERNAL_DATA,

    /**
     * Explicitly isolated test or demonstration data, strictly excluded from real municipal operations.
     */
    SIMULATED
}
