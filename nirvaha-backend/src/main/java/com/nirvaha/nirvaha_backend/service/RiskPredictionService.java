package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.WardRiskResponse;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;

/**
 * Abstraction layer for future AI/ML risk prediction delegation.
 * <p>
 * Java Layer: Owns domain logic, spatial aggregation, persistence, explanation, and policy.
 * Future Extension: Delegates advanced time-series or spatial forecasting to a Python AI service (e.g. POST /predict-risk)
 * without breaking domain contracts.
 */
public interface RiskPredictionService {

    WardRiskResponse predictWardRisk(Long wardId, IncidentCategory category);
}
