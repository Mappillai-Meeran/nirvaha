const now = new Date().toISOString();

export const DEMO_WORKSPACE = {
  incidents: [
    { id: 'DEMO-1042', title: 'Drain overflowing near Mission Street', category: 'DRAINAGE', severity: 'HIGH', status: 'REPORTED', address: 'Mission Street, Puducherry', latitude: 11.9350, longitude: 79.8300, originalLanguage: 'TA', createdAt: now, description: 'Drainage water is blocking the pedestrian crossing.' },
    { id: 'DEMO-1043', title: 'Streetlight out near beach road', category: 'STREETLIGHT', severity: 'MEDIUM', status: 'VERIFIED', address: 'Goubert Avenue, Puducherry', latitude: 11.9382, longitude: 79.8334, originalLanguage: 'EN', createdAt: now, description: 'The streetlight has been out for two nights.' },
    { id: 'DEMO-1044', title: 'Pothole at school junction', category: 'POTHOLE', severity: 'HIGH', status: 'IN_PROGRESS', address: 'M.G. Road, Puducherry', latitude: 11.9318, longitude: 79.8271, originalLanguage: 'HI', createdAt: now, description: 'Deep pothole creates a risk for two-wheelers.' },
  ],
  wards: [
    { wardId: 'PUD-05', wardNumber: '05', wardName: 'V.O.C. Nagar', latitude: 11.9350, longitude: 79.8300, riskScore100: 78, riskLevel: 'HIGH', topRiskCategory: 'DRAINAGE' },
    { wardId: 'PUD-11', wardNumber: '11', wardName: 'Goubert Nagar', latitude: 11.9382, longitude: 79.8334, riskScore100: 52, riskLevel: 'MODERATE', topRiskCategory: 'STREETLIGHT' },
    { wardId: 'PUD-18', wardNumber: '18', wardName: 'Orleanpet', latitude: 11.9318, longitude: 79.8271, riskScore100: 67, riskLevel: 'HIGH', topRiskCategory: 'POTHOLE' },
  ],
  signals: [
    { id: 'SIG-21', signalType: 'HOTSPOT', category: 'DRAINAGE', areaName: 'Mission Street', latitude: 11.9350, longitude: 79.8300, radiusMeters: 280, incidentCount: 6, unresolvedCount: 4, timeWindow: '14 days', explanation: 'Multiple drainage reports cluster near the pedestrian crossing.', modelVersion: 'demo-v1' },
    { id: 'SIG-22', signalType: 'EMERGING_ISSUE', category: 'POTHOLE', areaName: 'M.G. Road', latitude: 11.9318, longitude: 79.8271, radiusMeters: 220, incidentCount: 4, unresolvedCount: 3, timeWindow: '7 days', explanation: 'Road-surface complaints rose above the pilot baseline.', modelVersion: 'demo-v1' },
  ],
  actionQueue: {
    totalActionableCount: 2,
    criticalCount: 1,
    recommendations: [
      { id: 'REC-301', incidentId: 'DEMO-1042', incidentTitle: 'Drain overflowing near Mission Street', category: 'DRAINAGE', priorityLevel: 'HIGH', recommendedAction: 'FIELD_INSPECTION', ageHours: 8, slaState: 'AT_RISK', reason: 'Six linked reports and a high pedestrian exposure area require same-day inspection.', suggestedDepartment: 'Engineering', suggestedNextStep: 'Inspect inlet and arrange drain clearing.' },
      { id: 'REC-302', incidentId: 'DEMO-1044', incidentTitle: 'Pothole at school junction', category: 'POTHOLE', priorityLevel: 'HIGH', recommendedAction: 'ASSIGN_CREW', ageHours: 18, slaState: 'ON_TRACK', reason: 'School-route safety concern with verified location evidence.', suggestedDepartment: 'Roads', suggestedNextStep: 'Place temporary warning and issue repair work order.' },
    ],
  },
  learningSignals: [
    { id: 'LEARN-18', signalType: 'RECURRING_PROBLEM', category: 'DRAINAGE', areaName: 'Mission Street', sampleCount: 8, observation: 'Drainage reports return after short clearing cycles.', recommendedStrategy: 'Inspect the upstream inlet and add a scheduled pre-monsoon cleaning route.' },
  ],
  anomalies: [{ id: 'FLAG-9', anomalyType: 'REPEATED_REPORTING', incidentId: 'DEMO-1042' }],
  sources: [
    { id: 'DEMO-SOURCE', name: 'Nirvaha demonstration workspace', publisher: 'Nirvaha', classification: 'SIMULATED', description: 'Illustrative records shown because the local municipal API is offline. These are not live government cases.', license: 'Demo only' },
  ],
};

export const getDemoWardDetail = (wardId) => {
  const ward = DEMO_WORKSPACE.wards.find((item) => item.wardId === wardId) || DEMO_WORKSPACE.wards[0];
  return {
    ...ward,
    weatherData: { available: false },
    weatherContext: 'Live weather data is unavailable while the local API is offline.',
    factors: [
      { name: 'Open-case load', contribution: 0.32, value: 0.78 },
      { name: 'Severity', contribution: 0.24, value: 0.66 },
      { name: 'Duplicate reports', contribution: 0.18, value: 0.60 },
      { name: 'SLA pressure', contribution: 0.14, value: 0.52 },
      { name: 'Weather context', contribution: 0.07, value: 0.35 },
      { name: 'Resolution history', contribution: 0.05, value: 0.28 },
    ],
    explanation: 'This is a simulated pilot view designed to show how verified municipal data will be interpreted.',
    recommendedAction: 'Validate the reported location, assign the accountable department, and require field evidence before closure.',
  };
};
