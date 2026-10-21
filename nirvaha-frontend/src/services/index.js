import { apiClient } from './apiClient';

export const authService = {
  register: (account) => apiClient.post('/users', account),
  login: (credentials) => apiClient.post('/auth/login', credentials),
  getMe: () => apiClient.get('/auth/me'),
  changePassword: (data) => apiClient.post('/auth/change-password', data),
  forgotPassword: (data) => apiClient.post('/auth/forgot-password', data),
  resetPassword: (data) => apiClient.post('/auth/reset-password', data),
};

export const userService = {
  createUser: (userData) => apiClient.post('/users', userData),
  getUserById: (id) => apiClient.get(`/users/${id}`),
  getUserByEmail: (email) => apiClient.get(`/users/email/${email}`),
};

export const incidentService = {
  createIncident: (incidentData) => apiClient.post('/incidents', incidentData),
  getAllIncidents: (includeSimulated = false) => apiClient.get(`/incidents${includeSimulated ? '?includeSimulated=true' : ''}`),
  getMyReports: () => apiClient.get('/incidents/my-reports'),
  getIncidentById: (id) => apiClient.get(`/incidents/${id}`),
};

export const evidenceService = {
  uploadEvidence: (incidentId, evidenceData) => apiClient.upload(`/evidence/upload/incident/${incidentId}`, evidenceData),
  getEvidenceForIncident: (incidentId) => apiClient.get(`/evidence/incident/${incidentId}`),
};

export const duplicateService = {
  findDuplicates: (incidentId) => apiClient.get(`/incidents/${incidentId}/duplicates`),
};

export const priorityService = {
  evaluatePriority: (incidentId) => apiClient.post(`/incidents/${incidentId}/priority`),
  getPriority: (incidentId, rainfall) => {
    const query = rainfall != null ? `?rainfall=${rainfall}` : '';
    return apiClient.get(`/incidents/${incidentId}/priority${query}`);
  },
};

export const judgeDemoService = {
  getStatus: () => apiClient.get('/demo/status'),
  runMonsoonDrainageSurge: () => apiClient.post('/demo/scenarios/monsoon-drainage-surge'),
  runDuplicateDetection: () => apiClient.post('/demo/scenarios/duplicate-detection'),
  runResolutionFailure: () => apiClient.post('/demo/scenarios/resolution-failure'),
  resetDemo: (demoRunId) => apiClient.post(demoRunId ? `/demo/reset?demoRunId=${encodeURIComponent(demoRunId)}` : '/demo/reset'),
};

export const workflowService = {
  addAction: (incidentId, actionData) => apiClient.post(`/incidents/${incidentId}/actions`, actionData),
  getActionsForIncident: (incidentId) => apiClient.get(`/incidents/${incidentId}/actions`),
  getWorkflowForIncident: (incidentId) => apiClient.get(`/incidents/${incidentId}/workflow`),
  assignOfficer: (incidentId, officerId, remarks = 'Assigned to field officer') => 
    apiClient.post(`/incidents/${incidentId}/actions`, { officerId, actionType: 'ASSIGNED', remarks }),
  startWork: (incidentId, officerId, remarks = 'Field work initiated') => 
    apiClient.post(`/incidents/${incidentId}/actions`, { officerId, actionType: 'STARTED', remarks }),
  recordProgress: (incidentId, officerId, remarks) => 
    apiClient.post(`/incidents/${incidentId}/actions`, { officerId, actionType: 'PROGRESS_UPDATE', remarks }),
  resolveIncident: (incidentId, officerId, remarks = 'Resolved following field operations') => 
    apiClient.post(`/incidents/${incidentId}/actions`, { officerId, actionType: 'RESOLVED', remarks }),
  reopenIncident: (incidentId, officerId, remarks = 'Case reopened for re-inspection') => 
    apiClient.post(`/incidents/${incidentId}/actions`, { officerId, actionType: 'REOPENED', remarks }),
};

export const resolutionService = {
  submitResolution: (incidentId, resolutionData) => apiClient.post(`/incidents/${incidentId}/resolutions`, resolutionData),
  getResolutionForIncident: (incidentId) => apiClient.get(`/incidents/${incidentId}/resolution`),
};

export const integrityService = {
  detectAnomalies: (incidentId) => apiClient.post(`/integrity/evaluate/${incidentId}`),
  getAnomaliesForIncident: (incidentId) => apiClient.get(`/integrity/incident/${incidentId}`),
  getOpenAnomalies: () => apiClient.get('/integrity/open'),
};

export const riskService = {
  getWardRiskOverview: () => apiClient.get('/risk/wards'),
  getWardRiskDetail: (wardId, category = '') => {
    const query = category ? `?category=${encodeURIComponent(category)}` : '';
    return apiClient.get(`/risk/wards/${wardId}${query}`);
  },
  evaluateRisk: () => apiClient.post('/risk/assessments/evaluate', {}),
};

export const provenanceService = {
  getSources: () => apiClient.get('/provenance/sources'),
  getSourceById: (id) => apiClient.get(`/provenance/sources/${id}`),
  getAreas: () => apiClient.get('/provenance/areas'),
  getWeather: () => apiClient.get('/provenance/weather'),
  getLiveWeather: (lat, lon) => apiClient.get(`/provenance/weather/live${lat && lon ? `?latitude=${lat}&longitude=${lon}` : ''}`),
  getHistoricalWeather: (lat, lon, startDate, endDate) => {
    const params = new URLSearchParams();
    if (lat) params.append('latitude', lat);
    if (lon) params.append('longitude', lon);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    const query = params.toString();
    return apiClient.get(`/provenance/weather/history${query ? `?${query}` : ''}`);
  },
  getRoadSummary: () => apiClient.get('/provenance/roads/summary'),
  getNearestRoad: (lat, lon, maxRadius = 300) => apiClient.get(`/provenance/roads/nearest?latitude=${lat}&longitude=${lon}&maxRadiusMeters=${maxRadius}`),
  getRules: () => apiClient.get('/provenance/rules'),
};

export const intelligenceService = {
  getOverview: () => apiClient.get('/intelligence/overview'),
  getSignals: (params = {}) => {
    const query = new URLSearchParams(params).toString();
    return apiClient.get(`/intelligence/signals${query ? `?${query}` : ''}`);
  },
  getHotspots: () => apiClient.get('/intelligence/hotspots'),
  getEmergingIssues: () => apiClient.get('/intelligence/emerging-issues'),
  getUnresolvedLoad: () => apiClient.get('/intelligence/unresolved-load'),
  getResolutionPerformance: () => apiClient.get('/intelligence/resolution-performance'),
};

export const actionCenterService = {
  getQueue: (includeSimulated = false) => apiClient.get(`/action-center/queue${includeSimulated ? '?includeSimulated=true' : ''}`),
  getRecommendation: (incidentId) => apiClient.get(`/action-center/recommendations/${incidentId}`),
  recordDecision: (decisionData) => apiClient.post('/action-center/decisions', decisionData),
  getDecisionsForIncident: (incidentId) => apiClient.get(`/action-center/decisions/incident/${incidentId}`),
  getLearningSignals: () => apiClient.get('/action-center/learning-signals'),
  getClosedLoopChain: (incidentId) => apiClient.get(`/action-center/incidents/${incidentId}/full-chain`),
};

export const languageService = {
  getSupportedLanguages: () => apiClient.get('/language/supported'),
  detectLanguage: (text, userPreferredLang) => apiClient.post('/language/detect', { text, userPreferredLang }),
};

export const feedbackService = {
  submitFeedback: (incidentId, feedbackData) => apiClient.post(`/incidents/${incidentId}/feedback`, feedbackData),
  getFeedbackForIncident: (incidentId) => apiClient.get(`/incidents/${incidentId}/feedback`),
  reviewDispute: (incidentId, feedbackId, reviewData) => apiClient.post(`/incidents/${incidentId}/feedback/${feedbackId}/review`, reviewData),
};

export const notificationService = {
  getNotifications: (page = 0, size = 20) => apiClient.get(`/notifications?page=${page}&size=${size}`),
  getUnreadCount: () => apiClient.get('/notifications/unread-count'),
  markAsRead: (id) => apiClient.patch(`/notifications/${id}/read`),
  markAllAsRead: () => apiClient.patch('/notifications/read-all'),
};

export const systemService = {
  getIntegrationHealth: () => apiClient.get('/system/integration-health'),
};

export const slaService = {
  getStatus: () => apiClient.get('/sla/status'),
  getOverdue: () => apiClient.get('/sla/overdue'),
  getSummary: () => apiClient.get('/sla/summary'),
  getRules: () => apiClient.get('/sla/rules'),
  evaluate: () => apiClient.post('/sla/evaluate'),
};



