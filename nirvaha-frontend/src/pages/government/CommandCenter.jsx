import React, { useEffect, useState, useMemo } from 'react';
import { 
  incidentService, 
  riskService, 
  integrityService, 
  provenanceService, 
  intelligenceService, 
  actionCenterService,
  systemService
} from '../../services';
import { CommandMap } from '../../components/map/CommandMap';
import { Card, Badge, LoadingState, ErrorState, WeatherIndicator } from '../../components/ui';
import { 
  ShieldAlert, 
  Activity, 
  RefreshCcw, 
  ArrowRight, 
  Database, 
  ExternalLink, 
  Info, 
  Zap, 
  BookOpen, 
  Globe, 
  Search, 
  CheckCircle2, 
  FileText,
  AlertCircle,
  TrendingUp,
  Sparkles,
  X,
  Building2,
  Layers,
  Menu,
  CloudRain
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useLanguage } from '../../context/LanguageContext';
import { DEMO_WORKSPACE, getDemoWardDetail } from '../../data/demoWorkspace';
import { JudgeDemoModal } from '../../components/demo/JudgeDemoModal';
import { ExecutiveKpiCockpit } from '../../components/government/ExecutiveKpiCockpit';
import { MonsoonStressSimulator } from '../../components/government/MonsoonStressSimulator';
import { DepartmentRoutingMatrix } from '../../components/government/DepartmentRoutingMatrix';
import { DuplicateEquityRadar } from '../../components/government/DuplicateEquityRadar';
import { CommandCenterSidebar } from '../../components/government/CommandCenterSidebar';
import { WardRiskLeaderboard } from '../../components/government/WardRiskLeaderboard';
import { SlaTrackingPanel } from '../../components/government/SlaTrackingPanel';
import { ResolutionDonutChart } from '../../components/government/ResolutionDonutChart';
import { InfrastructureSpiderRadar } from '../../components/government/InfrastructureSpiderRadar';
import { PredominantRiskFactors } from '../../components/government/PredominantRiskFactors';
import { HistoricalPrecipitationPanel } from '../../components/government/HistoricalPrecipitationPanel';

export const CommandCenter = () => {
  const navigate = useNavigate();
  const { t } = useLanguage();

  // Core Data States
  const [incidents, setIncidents] = useState([]);
  const [wardRisks, setWardRisks] = useState([]);
  const [anomalies, setAnomalies] = useState([]);
  const [sources, setSources] = useState([]);
  const [intelSignals, setIntelSignals] = useState([]);
  const [actionQueue, setActionQueue] = useState(null);
  const [learningSignals, setLearningSignals] = useState([]);
  const [selectedWard, setSelectedWard] = useState(null);
  const [selectedWardDetail, setSelectedWardDetail] = useState(null);

  // Rainfall Simulation State
  const [simulatedRainMm, setSimulatedRainMm] = useState(0);
  const [simulatedWards, setSimulatedWards] = useState(null);
  const [simulatedIncidents, setSimulatedIncidents] = useState(null);

  const handleSimulationChange = (mm, simWards, simIncs) => {
    setSimulatedRainMm(mm);
    setSimulatedWards(mm > 0 ? simWards : null);
    setSimulatedIncidents(mm > 0 ? simIncs : null);
  };

  // Status & Mode States
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isOfflineDemo, setIsOfflineDemo] = useState(false);
  const [activeLayers, setActiveLayers] = useState({ incidents: true, risk: true, hotspots: true });
  const [activeTab, setActiveTab] = useState('recommendations');
  const [activeNav, setActiveNav] = useState('map'); // 'map' | 'recommendations' | 'all-incidents' | 'integrity' | 'jurisdiction-matrix' | 'duplicate-equity' | 'simulator' | 'executive-cockpit'
  const [mapViewMode, setMapViewMode] = useState('points'); // 'points' | 'heatmap'
  const [sidebarMobileOpen, setSidebarMobileOpen] = useState(false);

  // Filter States
  const [languageFilter, setLanguageFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [severityFilter, setSeverityFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const [includeSimulated, setIncludeSimulated] = useState(false);

  // Modals
  const [showJudgeDemoModal, setShowJudgeDemoModal] = useState(false);
  const [showProvenanceModal, setShowProvenanceModal] = useState(false);
  const [showIntegrationModal, setShowIntegrationModal] = useState(false);
  const [integrationHealth, setIntegrationHealth] = useState(null);
  const [selectedRec, setSelectedRec] = useState(null);
  const [decisionType, setDecisionType] = useState('ACCEPT');
  const [decisionReason, setDecisionReason] = useState('');
  const [decisionSubmitting, setDecisionSubmitting] = useState(false);

  const fetchData = async (simFlag = includeSimulated) => {
    setLoading(true);
    setError(null);
    try {
      let incData = null;
      let incError = null;
      try {
        incData = await incidentService.getAllIncidents(simFlag);
      } catch (err) {
        incError = err;
      }

      if (incError) {
        if (incError.status === 401) {
          setError('Authentication session expired or missing. Please sign in again.');
        } else if (incError.status === 403) {
          setError('Access denied: Officer or Administrator privileges required to access operational telemetry.');
        } else {
          setError(incError.message || 'Failed to sync live operational telemetry.');
        }
        setIsOfflineDemo(false);
        setLoading(false);
        return;
      }

      const [
        riskData, 
        anomalyData, 
        sourceData, 
        _intelData, 
        signalData, 
        queueData, 
        learnData,
        healthData
      ] = await Promise.all([
        riskService.getWardRiskOverview().catch(() => null),
        integrityService.getOpenAnomalies().catch(() => null),
        provenanceService.getSources().catch(() => null),
        intelligenceService.getOverview().catch(() => null),
        intelligenceService.getSignals().catch(() => null),
        actionCenterService.getQueue(simFlag).catch(() => null),
        actionCenterService.getLearningSignals().catch(() => null),
        systemService.getIntegrationHealth().catch(() => null),
      ]);

      if (healthData) {
        setIntegrationHealth(healthData);
      }

      // Check live API responses
      if (Array.isArray(incData)) {
        setIncidents(incData);
        setWardRisks(Array.isArray(riskData) ? riskData : []);
        setAnomalies(Array.isArray(anomalyData) ? anomalyData : []);
        setSources(Array.isArray(sourceData) ? sourceData : []);
        setIntelSignals(Array.isArray(signalData) ? signalData : []);
        setActionQueue(queueData || null);
        setLearningSignals(Array.isArray(learnData) ? learnData : []);

        if (Array.isArray(riskData) && riskData.length > 0) {
          setSelectedWard(riskData[0]);
          fetchWardDetail(riskData[0].wardId, false);
        }
        setIsOfflineDemo(false);
      } else {
        setError('Operational telemetry service returned an unexpected response format.');
        setIsOfflineDemo(false);
      }
    } catch (err) {
      setError('Failed to sync live operational telemetry: ' + (err.message || 'Network error'));
      setIsOfflineDemo(false);
    } finally {
      setLoading(false);
    }
  };

  const loadDemoWorkspace = () => {
    setIncidents(DEMO_WORKSPACE.incidents);
    setWardRisks(DEMO_WORKSPACE.wards);
    setAnomalies(DEMO_WORKSPACE.anomalies);
    setSources(DEMO_WORKSPACE.sources);
    setIntelSignals(DEMO_WORKSPACE.signals);
    setActionQueue(DEMO_WORKSPACE.actionQueue);
    setLearningSignals(DEMO_WORKSPACE.learningSignals);
    setSelectedWard(DEMO_WORKSPACE.wards[0]);
    setSelectedWardDetail(getDemoWardDetail(DEMO_WORKSPACE.wards[0].wardId));
    setIsOfflineDemo(true);
  };

  const fetchWardDetail = async (wardId, offlineMode = isOfflineDemo) => {
    if (offlineMode) {
      setSelectedWardDetail(getDemoWardDetail(wardId));
      return;
    }
    try {
      const detail = await riskService.getWardRiskDetail(wardId);
      setSelectedWardDetail(detail);
    } catch (err) {
      console.warn('Failed to load detailed ward risk, using baseline fallback:', err);
      setSelectedWardDetail(getDemoWardDetail(wardId));
    }
  };

  const handleRecordDecision = async () => {
    if (!selectedRec) return;
    if (decisionType !== 'ACCEPT' && (!decisionReason || !decisionReason.trim())) {
      alert('A valid operational rationale is required when modifying, deferring, or rejecting a recommendation.');
      return;
    }
    setDecisionSubmitting(true);
    try {
      await actionCenterService.recordDecision({
        incidentId: typeof selectedRec.incidentId === 'number' ? selectedRec.incidentId : parseInt(selectedRec.incidentId, 10) || 1,
        recommendationId: selectedRec.id,
        recommendedAction: selectedRec.recommendedAction,
        decision: decisionType,
        reason: decisionReason
      });
      setSelectedRec(null);
      setDecisionReason('');
      fetchData();
    } catch (err) {
      alert(err.message || 'Failed to record officer decision');
    } finally {
      setDecisionSubmitting(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    const timer = setTimeout(() => {
      if (isMounted) {
        fetchData();
      }
    }, 0);
    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, []);

  // Filtered Incidents based on Category, Status, Severity, Language, and Search Query
  const filteredIncidents = useMemo(() => {
    return incidents.filter((inc) => {
      // Language filter
      if (languageFilter !== 'ALL' && (inc.originalLanguage || 'EN') !== languageFilter) {
        return false;
      }
      // Category filter
      if (categoryFilter !== 'ALL' && inc.category !== categoryFilter) {
        return false;
      }
      // Status filter
      if (statusFilter !== 'ALL' && inc.status !== statusFilter) {
        return false;
      }
      // Severity filter
      if (severityFilter !== 'ALL' && inc.severity !== severityFilter) {
        return false;
      }
      // Search query (ID, title, description, address)
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesId = inc.id?.toString().toLowerCase().includes(q);
        const matchesTitle = inc.title?.toLowerCase().includes(q);
        const matchesDesc = (inc.originalDescription || inc.description || '').toLowerCase().includes(q);
        const matchesAddr = (inc.address || '').toLowerCase().includes(q);
        if (!matchesId && !matchesTitle && !matchesDesc && !matchesAddr) {
          return false;
        }
      }
      return true;
    });
  }, [incidents, languageFilter, categoryFilter, statusFilter, severityFilter, searchQuery]);

  // High & Critical metrics derived from verified state
  const activeCases = useMemo(() => {
    return incidents.filter((i) => i.status === 'REPORTED' || i.status === 'VERIFIED' || i.status === 'IN_PROGRESS');
  }, [incidents]);

  const resolvedCases = useMemo(() => {
    return incidents.filter((i) => i.status === 'RESOLVED');
  }, [incidents]);

  const overdueSlaCount = useMemo(() => {
    if (actionQueue?.recommendations) {
      return actionQueue.recommendations.filter((r) => r.slaStatus === 'OVERDUE' || r.slaStatus === 'BREACHED' || r.isOverdue).length;
    }
    return incidents.filter((i) => i.slaStatus === 'OVERDUE' || i.slaBreached).length;
  }, [actionQueue, incidents]);

  const fieldInspectionsCount = useMemo(() => {
    if (actionQueue?.recommendations) {
      const insp = actionQueue.recommendations.filter((r) => r.recommendedAction === 'FIELD_INSPECTION');
      return insp.length > 0 ? insp.length : actionQueue.totalActionableCount || 0;
    }
    return incidents.filter((i) => i.status === 'IN_PROGRESS' || i.status === 'VERIFIED').length;
  }, [actionQueue, incidents]);

  const criticalRiskWards = (simulatedWards || wardRisks).filter((w) => w.riskLevel === 'CRITICAL' || w.riskLevel === 'HIGH').length;
  const criticalIncidentsCount = incidents.filter((i) => i.severity === 'CRITICAL' || i.severity === 'HIGH').length;
  const actionableCount = actionQueue?.totalActionableCount || 0;
  const criticalActionCount = actionQueue?.criticalCount || 0;

  if (loading) return <LoadingState message="Connecting to Puducherry Command Center telemetry..." />;
  if (error) return <ErrorState message={error} onRetry={fetchData} />;

  return (
    <div className="flex h-[calc(100vh-100px)] bg-[#F4F7F5] w-full overflow-hidden">
      {/* 1. Persistent Left Command Sidebar (Operations, Intelligence, Governance) */}
      <CommandCenterSidebar 
        activeNav={activeNav}
        onSelectNav={(nav) => {
          setActiveNav(nav);
          setActiveTab(nav);
        }}
        actionCount={actionableCount}
        incidentCount={incidents.length}
        anomalyCount={anomalies.length}
        signalCount={intelSignals.length}
        onOpenJudgeDemo={() => setShowJudgeDemoModal(true)}
        onOpenProvenance={() => setShowProvenanceModal(true)}
        isSimulationActive={simulatedRainMm > 0}
        isMobileOpen={sidebarMobileOpen}
        onCloseMobile={() => setSidebarMobileOpen(false)}
      />

      {/* 2. Main Command & Analytics Workspace */}
      <main className="flex-1 flex flex-col p-3 sm:p-5 space-y-4 overflow-y-auto h-full min-w-0">
        
        {/* Official Operational Context & Mode Banner */}
        <div className="flex flex-col gap-2">
          {error && !isOfflineDemo && (
            <div className="border border-red-300 bg-red-50 rounded-lg p-2.5 text-xs text-red-900 flex items-center justify-between gap-3 shadow-xs" role="alert">
              <div className="flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                <span>{error}</span>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button 
                  onClick={fetchData} 
                  className="px-2.5 py-1 bg-red-700 hover:bg-red-800 text-white font-bold text-[11px] rounded transition shadow-xs"
                >
                  Retry Connection
                </button>
                <button 
                  onClick={loadDemoWorkspace} 
                  className="px-2.5 py-1 bg-white hover:bg-red-100 text-red-800 border border-red-300 font-bold text-[11px] rounded transition shadow-xs"
                >
                  Load Demo Workspace
                </button>
              </div>
            </div>
          )}

          {isOfflineDemo ? (
            <div className="border border-[#e2b66b] bg-[#fff8e8] rounded-lg p-2.5 text-xs text-[#5d4a2e] flex items-center justify-between gap-3 shadow-xs" role="status">
              <div className="flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-[#a36711] shrink-0" />
                <span>
                  <strong>Demonstration Data Mode:</strong> Local Spring Boot backend is unreachable on port 8080. 
                  Displaying deterministic municipal pilot dataset for Puducherry.
                </span>
              </div>
              <button 
                onClick={fetchData} 
                className="px-2.5 py-1 bg-[#8a5411] hover:bg-[#72450c] text-white font-bold text-[11px] rounded transition shadow-xs shrink-0"
              >
                Retry Live Telemetry
              </button>
            </div>
          ) : (
            <div className="bg-[#102D27] text-white border border-[#17483B] rounded-lg p-2.5 text-xs flex items-center justify-between shadow-xs">
              <div className="flex items-center space-x-2.5">
                <Info className="w-4 h-4 text-[#34D399] shrink-0" />
                <span>
                  <strong className="text-white font-semibold">Government of Puducherry • Integrated Municipal Operations Command (ICCC):</strong> Spatial civic intelligence, pattern telemetry, and officer decision support.
                </span>
              </div>
              <div className="flex items-center space-x-1.5 font-mono text-[10px] text-[#34D399] shrink-0">
                <span className="w-2 h-2 rounded-full bg-[#34D399] animate-pulse" />
                <span>LIVE MUNICIPAL TELEMETRY CONNECTED</span>
              </div>
            </div>
          )}

          {includeSimulated && (
            <div className="border border-purple-300 bg-purple-50 rounded-lg p-2.5 text-xs text-purple-900 flex items-center justify-between gap-3 shadow-xs" role="status">
              <div className="flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 text-purple-700 shrink-0" />
                <span>
                  <strong>Simulated Demonstration Data Included:</strong> Currently viewing test & simulated scenario incidents alongside official citizen records. Simulated records are isolated and not official public complaints.
                </span>
              </div>
              <button
                onClick={() => {
                  setIncludeSimulated(false);
                  fetchData(false);
                }}
                className="px-2.5 py-1 bg-purple-700 hover:bg-purple-800 text-white font-bold text-[11px] rounded transition shadow-xs shrink-0"
              >
                Show Official Only
              </button>
            </div>
          )}

          {/* Top Operational Header Bar */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 bg-white p-3.5 rounded-lg border border-[#E2E8E5] shadow-xs">
            <div className="flex items-center gap-3">
              <button 
                onClick={() => setSidebarMobileOpen(true)}
                className="md:hidden p-2 rounded-lg bg-[#F4F7F5] text-[#18201F] hover:bg-[#E2E8E5]"
                aria-label="Open command menu"
              >
                <Menu size={18} />
              </button>
              <div>
                <div className="flex items-center space-x-2">
                  <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-[#18201F]">
                    {t('command.title')}
                  </h1>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-[#102D27] text-[#34D399] font-bold border border-[#17483B]">
                    ICCC-PUDUCHERRY
                  </span>
                </div>
                <p className="text-xs text-[#66716E] mt-0.5">
                  Territorial Municipal Command • Multi-Ward Spatial Intelligence & Triaging
                </p>
              </div>
            </div>

            <div className="flex items-center flex-wrap gap-2">
              {/* Language Filter */}
              <div className="flex items-center space-x-1.5 bg-[#F4F7F5] border border-[#E2E8E5] px-2.5 py-1.5 rounded text-xs">
                <Globe className="w-3.5 h-3.5 text-[#17483B]" />
                <span className="text-[#66716E] font-mono text-[11px]">{t('command.languageFilter')}:</span>
                <select
                  value={languageFilter}
                  onChange={(e) => setLanguageFilter(e.target.value)}
                  className="bg-transparent text-[#17483B] font-bold focus:outline-none cursor-pointer"
                  aria-label="Filter incidents by reporting language"
                >
                  <option value="ALL">All Languages</option>
                  <option value="TA">Tamil (தமிழ்)</option>
                  <option value="HI">Hindi (हिन्दी)</option>
                  <option value="ML">Malayalam (മലയാളം)</option>
                  <option value="TE">Telugu (తెలుగు)</option>
                  <option value="EN">English</option>
                </select>
              </div>

              {/* Simulated Dataset Filter Toggle */}
              <button
                onClick={() => {
                  const nextSim = !includeSimulated;
                  setIncludeSimulated(nextSim);
                  fetchData(nextSim);
                }}
                className={`px-3 py-1.5 border text-xs font-semibold rounded flex items-center space-x-1.5 transition shadow-xs ${
                  includeSimulated
                    ? 'bg-purple-100 text-purple-900 border-purple-300 font-bold'
                    : 'bg-white hover:bg-[#F4F7F5] text-[#66716E] border-[#E2E8E5]'
                }`}
                title="Toggle between official civic records and simulated demonstration cases"
              >
                <Layers className="w-3.5 h-3.5" />
                <span>{includeSimulated ? 'Showing: Simulated + Official' : 'Official Records Only'}</span>
              </button>

              <button
                onClick={() => setShowJudgeDemoModal(true)}
                className="px-3 py-1.5 bg-[#C58A32] hover:bg-[#a67225] text-white text-xs font-bold rounded flex items-center space-x-1.5 transition shadow-xs"
                title="Open Judge Demonstration Mode for Hackathon Presentation"
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>Judge Demo Mode</span>
              </button>

              <button
                onClick={() => setShowIntegrationModal(true)}
                className={`px-3 py-1.5 bg-white hover:bg-[#F4F7F5] border text-xs font-semibold rounded flex items-center space-x-1.5 transition shadow-xs ${
                  integrationHealth?.systemStatus === 'CONNECTED' || integrationHealth?.systemStatus === 'HEALTHY'
                    ? 'border-emerald-300 text-emerald-800'
                    : 'border-amber-300 text-amber-800'
                }`}
                title="Inspect real-world data integrations and API health"
              >
                <Activity className="w-3.5 h-3.5" />
                <span>
                  Integrations ({integrationHealth?.integrations ? integrationHealth.integrations.length : 5})
                </span>
                <span className={`w-2 h-2 rounded-full ${
                  integrationHealth?.systemStatus === 'CONNECTED' || integrationHealth?.systemStatus === 'HEALTHY'
                    ? 'bg-emerald-500'
                    : 'bg-amber-500 animate-pulse'
                }`} />
              </button>

              <button
                onClick={() => setShowProvenanceModal(true)}
                className="px-3 py-1.5 bg-white hover:bg-[#F4F7F5] text-[#18201F] border border-[#E2E8E5] text-xs font-semibold rounded flex items-center space-x-1.5 transition shadow-xs"
                title="Inspect dataset classifications and licensing"
              >
                <Database className="w-3.5 h-3.5 text-[#B94D36]" />
                <span>Registry ({sources.length})</span>
              </button>

              <button
                onClick={fetchData}
                className="px-3 py-1.5 bg-[#17483B] hover:bg-[#102D27] text-white text-xs font-bold rounded flex items-center space-x-1.5 transition shadow-xs"
              >
                <RefreshCcw className="w-3.5 h-3.5" />
                <span>{t('command.sync')}</span>
              </button>
            </div>
          </div>
        </div>

        {/* 3. Sentinella 6-Card Executive KPI Strip */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          {/* Card 1: Vistorias / Field Inspections */}
          <div className="bg-white border border-[#E2E8E5] rounded-lg p-3 shadow-xs flex flex-col justify-between hover:border-[#17483B]/30 transition">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-[#66716E]">Field Inspections</span>
              <span className="w-2 h-2 rounded-full bg-[#64748B]" />
            </div>
            <div className="my-1.5">
              <span className="text-2xl font-black font-mono text-[#18201F]">{fieldInspectionsCount}</span>
            </div>
            <span className="text-[10px] text-[#66716E]">Scheduled / Active</span>
          </div>

          {/* Card 2: Focos registrados / Total Reports */}
          <div className="bg-white border border-[#E2E8E5] rounded-lg p-3 shadow-xs flex flex-col justify-between hover:border-[#17483B]/30 transition">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-[#66716E]">Total Reports</span>
              <span className="w-2 h-2 rounded-full bg-[#0284C7]" />
            </div>
            <div className="my-1.5">
              <span className="text-2xl font-black font-mono text-[#18201F]">{incidents.length}</span>
            </div>
            <span className="text-[10px] text-[#0284C7] font-semibold">100% telemetry synced</span>
          </div>

          {/* Card 3: Focos ativos / Active Cases (Red) */}
          <div className="bg-white border border-[#E2E8E5] rounded-lg p-3 shadow-xs flex flex-col justify-between hover:border-[#DC2626]/30 transition">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-[#66716E]">Active Cases</span>
              <span className="w-2 h-2 rounded-full bg-[#DC2626] animate-pulse" />
            </div>
            <div className="my-1.5">
              <span className="text-2xl font-black font-mono text-[#DC2626]">{activeCases.length}</span>
            </div>
            <span className="text-[10px] text-[#DC2626] font-semibold">{criticalIncidentsCount} critical / high</span>
          </div>

          {/* Card 4: Focos resolvidos / Verified Resolutions (Green) */}
          <div className="bg-white border border-[#E2E8E5] rounded-lg p-3 shadow-xs flex flex-col justify-between hover:border-[#15803D]/30 transition">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-[#66716E]">Verified Resolutions</span>
              <span className="w-2 h-2 rounded-full bg-[#15803D]" />
            </div>
            <div className="my-1.5">
              <span className="text-2xl font-black font-mono text-[#15803D]">{resolvedCases.length}</span>
            </div>
            <span className="text-[10px] text-[#15803D] font-semibold">
              {incidents.length > 0 ? Math.round((resolvedCases.length / incidents.length) * 100) : 0}% verified rate
            </span>
          </div>

          {/* Card 5: SLA vencidos / Overdue SLAs (Amber) */}
          <div 
            onClick={() => setActiveNav('sla-tracking')}
            className="bg-white border border-[#E2E8E5] rounded-lg p-3 shadow-xs flex flex-col justify-between hover:border-[#D97706]/60 transition cursor-pointer group"
            title="Inspect statutory SLA compliance and overdue cases"
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-[#66716E] group-hover:text-[#D97706] transition">Overdue SLAs</span>
              <span className="w-2 h-2 rounded-full bg-[#D97706]" />
            </div>
            <div className="my-1.5">
              <span className="text-2xl font-black font-mono text-[#D97706]">{overdueSlaCount}</span>
            </div>
            <span className="text-[10px] text-[#D97706] font-semibold flex items-center justify-between">
              <span>{overdueSlaCount === 0 ? 'Compliant' : 'Requires escalation'}</span>
              <span className="text-[9px] underline">Inspect →</span>
            </span>
          </div>

          {/* Card 6: Grupos vulneráveis / Vulnerable Zones (Blue) */}
          <div className="bg-white border border-[#E2E8E5] rounded-lg p-3 shadow-xs flex flex-col justify-between hover:border-[#2563EB]/30 transition">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-[#66716E]">Vulnerable Zones</span>
              <span className="w-2 h-2 rounded-full bg-[#2563EB]" />
            </div>
            <div className="my-1.5">
              <span className="text-2xl font-black font-mono text-[#2563EB]">{criticalRiskWards}</span>
            </div>
            <span className="text-[10px] text-[#2563EB] font-semibold">of {wardRisks.length} municipal wards</span>
          </div>
        </div>

        {/* SLA Tracking View (when activeNav is sla-tracking) */}
        {activeNav === 'sla-tracking' && (
          <SlaTrackingPanel onSelectIncident={(id) => navigate(`/incidents/${id}`)} />
        )}

        {/* 4. Executive Cockpit View (when activeNav is executive-cockpit) */}
        {activeNav === 'executive-cockpit' && (
          <ExecutiveKpiCockpit 
            incidents={incidents}
            actionQueue={actionQueue}
            learningSignals={learningSignals}
            anomalies={anomalies}
          />
        )}

        {/* 5. Monsoon Stress Simulator View (when activeNav is simulator) */}
        {activeNav === 'simulator' && (
          <Card className="bg-white border border-[#E2E8E5] p-5 shadow-xs space-y-4">
            <div className="border-b border-[#E2E8E5] pb-3">
              <h2 className="text-base font-bold text-[#18201F] flex items-center gap-2">
                <CloudRain className="w-5 h-5 text-[#2563EB]" />
                <span>Puducherry Predictive Monsoon & Hydrological Stress Simulator</span>
              </h2>
              <p className="text-xs text-[#66716E] mt-0.5">
                Dynamic precipitation modeling with physical elevation factors and drainage capacity stress simulation.
              </p>
            </div>
            <MonsoonStressSimulator 
              wards={wardRisks}
              incidents={incidents}
              onSimulateChange={handleSimulationChange}
            />
          </Card>
        )}

        {/* Historical Precipitation Telemetry View (when activeNav is historical-rainfall) */}
        {(activeNav === 'historical-rainfall' || activeTab === 'historical-rainfall') && (
          <HistoricalPrecipitationPanel 
            defaultLat={11.9156}
            defaultLon={79.8296}
            onClose={() => {
              setActiveNav('map');
              setActiveTab('map');
            }}
          />
        )}

        {/* 6. Primary Spatial Operations Grid (when activeNav is map) */}
        {activeNav === 'map' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-stretch">
            
            {/* GIS Operational Map Column (7 Cols) */}
            <div className="lg:col-span-7 bg-white border border-[#E2E8E5] rounded-lg p-3.5 space-y-3 shadow-xs flex flex-col h-full">
              {/* Map Header & Controls */}
              <div className="flex flex-wrap items-center justify-between border-b border-[#E2E8E5] pb-2.5 gap-2">
                <div className="flex items-center gap-2">
                  <Activity className="w-4 h-4 text-[#17483B]" />
                  <span className="text-xs font-bold uppercase tracking-wider text-[#18201F]">
                    Municipal GIS Operational Radar
                  </span>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-[#F4F7F5] text-[#17483B] font-mono font-bold border border-[#E2E8E5]">
                    {incidents.length} Registrados
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  {/* Pontos / Mapa de Calor toggle */}
                  <div className="inline-flex rounded-md p-0.5 bg-[#F4F7F5] border border-[#E2E8E5] text-[11px]">
                    <button
                      onClick={() => setMapViewMode('points')}
                      className={`px-2.5 py-1 rounded font-semibold transition ${
                        mapViewMode === 'points'
                          ? 'bg-white text-[#17483B] shadow-xs'
                          : 'text-[#66716E] hover:text-[#18201F]'
                      }`}
                    >
                      Pontos
                    </button>
                    <button
                      onClick={() => setMapViewMode('heatmap')}
                      className={`px-2.5 py-1 rounded font-semibold transition ${
                        mapViewMode === 'heatmap'
                          ? 'bg-white text-[#17483B] shadow-xs'
                          : 'text-[#66716E] hover:text-[#18201F]'
                      }`}
                    >
                      Mapa de Calor
                    </button>
                  </div>

                  {/* Layer Checkboxes */}
                  <div className="hidden sm:flex items-center space-x-2 text-xs">
                    <label className="flex items-center space-x-1 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={activeLayers.incidents}
                        onChange={(e) => setActiveLayers({ ...activeLayers, incidents: e.target.checked })}
                        className="rounded border-[#E2E8E5] text-[#17483B]"
                      />
                      <span className="text-[#66716E] font-mono text-[10px]">Incidents ({incidents.length})</span>
                    </label>

                    <label className="flex items-center space-x-1 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={activeLayers.risk}
                        onChange={(e) => setActiveLayers({ ...activeLayers, risk: e.target.checked })}
                        className="rounded border-[#E2E8E5] text-[#D97706]"
                      />
                      <span className="text-[#66716E] font-mono text-[10px]">Wards ({wardRisks.length})</span>
                    </label>

                    <label className="flex items-center space-x-1 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={activeLayers.hotspots}
                        onChange={(e) => setActiveLayers({ ...activeLayers, hotspots: e.target.checked })}
                        className="rounded border-[#E2E8E5] text-[#DC2626]"
                      />
                      <span className="text-[#66716E] font-mono text-[10px]">Hotspots ({intelSignals.length})</span>
                    </label>
                  </div>
                </div>
              </div>

              {/* Integrated Monsoon Simulator */}
              <MonsoonStressSimulator 
                wards={wardRisks}
                incidents={incidents}
                onSimulateChange={handleSimulationChange}
              />

              {/* Map Canvas with Floating Risk Legend */}
              <div className="flex-1 min-h-[380px] rounded-lg overflow-hidden border border-[#E2E8E5] relative">
                <CommandMap
                  incidents={languageFilter === 'ALL' ? (simulatedIncidents || incidents) : (simulatedIncidents || incidents).filter(i => (i.originalLanguage || 'EN') === languageFilter)}
                  wards={simulatedWards || wardRisks}
                  signals={intelSignals}
                  selectedWard={selectedWard}
                  activeLayers={activeLayers}
                  onSelectWard={(ward) => {
                    setSelectedWard(ward);
                    fetchWardDetail(ward.wardId);
                  }}
                  onSelectIncident={(inc) => navigate(`/incidents/${inc.id}`)}
                />

                {/* Floating In-Map Risk Legend */}
                <div className="absolute bottom-3 left-3 z-[1000] bg-white/95 backdrop-blur-xs border border-[#E2E8E5] rounded-md px-3 py-2 shadow-md text-[10px] space-y-1">
                  <span className="font-bold uppercase tracking-wider text-[#66716E] block font-mono text-[9px]">
                    Nível de Risco
                  </span>
                  <div className="flex items-center gap-3 font-semibold text-[#18201F]">
                    <span className="flex items-center gap-1">
                      <span className="w-2.5 h-2.5 rounded-full bg-[#15803D]" />
                      <span>Baixo</span>
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-2.5 h-2.5 rounded-full bg-[#D97706]" />
                      <span>Moderado</span>
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-2.5 h-2.5 rounded-full bg-[#EA580C]" />
                      <span>Alto</span>
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-2.5 h-2.5 rounded-full bg-[#DC2626]" />
                      <span>Crítico</span>
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-2.5 h-2.5 rounded-full bg-[#0284C7]" />
                      <span>Radar</span>
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between text-[11px] font-mono text-[#66716E] pt-1">
                <span>Center: Puducherry Municipal Area (11.9350° N, 79.8300° E)</span>
                <span>Click any ward or incident pin to inspect details</span>
              </div>
            </div>

            {/* Right Analytics Panel (5 Cols) */}
            <div className="lg:col-span-5 space-y-4">
              {/* 1. Ward Risk Leaderboard */}
              <WardRiskLeaderboard 
                wards={simulatedWards || wardRisks}
                selectedWard={selectedWard}
                onSelectWard={(ward) => {
                  setSelectedWard(ward);
                  fetchWardDetail(ward.wardId);
                }}
              />

              {/* 2. Resolution Verification Donut */}
              <ResolutionDonutChart incidents={incidents} />

              {/* 3. 5-Axis Spider Radar */}
              <InfrastructureSpiderRadar 
                selectedWardDetail={selectedWardDetail} 
                allWards={wardRisks} 
              />

              {/* 4. Predominant Risk Factors */}
              <PredominantRiskFactors 
                incidents={incidents} 
                wardDetail={selectedWardDetail} 
              />

              {/* 5. Selected Ward Detail Breakdown & Weather */}
              {selectedWardDetail && (
                <Card className="space-y-3.5 border-[#E2E8E5] bg-white shadow-xs">
                  <div className="flex items-start justify-between border-b border-[#E2E8E5] pb-3">
                    <div>
                      <span className="text-[10px] text-[#66716E] font-mono font-bold">MUNICIPAL GAZETTE WARD #{selectedWardDetail.wardNumber}</span>
                      <h3 className="text-base font-bold text-[#18201F] leading-tight">{selectedWardDetail.wardName}</h3>
                      <div className="text-xs text-[#17483B] font-semibold mt-0.5">Primary Risk: {selectedWardDetail.topRiskCategory}</div>
                    </div>

                    <div className="text-right">
                      <div className="text-2xl font-black text-[#18201F] font-mono">
                        {selectedWardDetail.riskScore100}
                        <span className="text-xs font-normal text-[#66716E]">/100</span>
                      </div>
                      <Badge 
                        variant={
                          selectedWardDetail.riskLevel === 'CRITICAL' ? 'critical' :
                          selectedWardDetail.riskLevel === 'HIGH' ? 'warning' : 'success'
                        }
                      >
                        {selectedWardDetail.riskLevel} RISK
                      </Badge>
                    </div>
                  </div>

                  {/* Weather Telemetry */}
                  <WeatherIndicator
                    weatherData={selectedWardDetail.weatherData || { available: false }}
                    weatherContext={selectedWardDetail.weatherContext}
                  />

                  {/* 6-Signal Factor Contribution Formula */}
                  <div className="space-y-2">
                    <span className="text-[10px] font-mono text-[#66716E] uppercase tracking-wider block font-bold">
                      6-Signal Risk Contribution Formula
                    </span>
                    <div className="space-y-1.5 text-xs">
                      {selectedWardDetail.factors && selectedWardDetail.factors.map((f, idx) => (
                        <div key={idx} className="space-y-1">
                          <div className="flex justify-between text-[#18201F] text-[11px]">
                            <span>{f.name}</span>
                            <span className="font-mono text-[#66716E]">{(f.contribution * 100).toFixed(1)}%</span>
                          </div>
                          <div className="w-full bg-[#F4F7F5] rounded-full h-1.5 border border-[#E2E8E5] overflow-hidden">
                            <div 
                              className="bg-[#17483B] h-1.5 rounded-full" 
                              style={{ width: `${Math.min(100, f.value * 100)}%` }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Algorithmic Rationale */}
                  <div className="bg-[#F4F7F5] border border-[#E2E8E5] rounded p-3 text-xs space-y-1">
                    <span className="font-bold text-[#17483B] block text-[11px] uppercase tracking-wider">
                      Algorithmic Rationale
                    </span>
                    <p className="leading-relaxed text-[#18201F] text-[11px]">{selectedWardDetail.explanation}</p>
                  </div>

                  {/* Decision Support Recommendation */}
                  <div className="bg-[#17483B]/5 border border-[#17483B]/20 rounded p-3 text-xs text-[#17483B] space-y-1">
                    <span className="font-bold text-[#17483B] block uppercase tracking-wider text-[10px]">
                      Recommended Operational Action
                    </span>
                    <p className="leading-relaxed text-[11px] text-[#18201F]">{selectedWardDetail.recommendedAction}</p>
                  </div>
                </Card>
              )}
            </div>

          </div>
        )}

      {/* 4. Tabbed Operational Command Section: Queue vs Incidents vs Integrity */}
      <div className="space-y-3">
        {/* Navigation Tabs Header */}
        <div className="flex border-b border-[#D9DEDB] items-center justify-between overflow-x-auto">
          <div className="flex space-x-2">
            <button
              onClick={() => setActiveTab('recommendations')}
              className={`px-4 py-2.5 text-xs font-bold border-b-2 transition flex items-center space-x-2 whitespace-nowrap ${
                activeTab === 'recommendations'
                  ? 'border-[#b94d36] text-[#b94d36]'
                  : 'border-transparent text-[#66716E] hover:text-[#18201F]'
              }`}
            >
              <Zap size={14} />
              <span>Action Recommendations ({actionQueue?.totalActionableCount || 0})</span>
            </button>

            <button
              onClick={() => setActiveTab('jurisdiction-matrix')}
              className={`px-4 py-2.5 text-xs font-bold border-b-2 transition flex items-center space-x-2 whitespace-nowrap ${
                activeTab === 'jurisdiction-matrix'
                  ? 'border-[#174f4a] text-[#174f4a]'
                  : 'border-transparent text-[#66716E] hover:text-[#18201F]'
              }`}
            >
              <Building2 size={14} />
              <span>Inter-Department Routing</span>
            </button>

            <button
              onClick={() => setActiveTab('duplicate-equity')}
              className={`px-4 py-2.5 text-xs font-bold border-b-2 transition flex items-center space-x-2 whitespace-nowrap ${
                activeTab === 'duplicate-equity'
                  ? 'border-[#b94d36] text-[#b94d36]'
                  : 'border-transparent text-[#66716E] hover:text-[#18201F]'
              }`}
            >
              <Layers size={14} />
              <span>Duplicate Clusters & Equity Radar ({intelSignals.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('all-incidents')}
              className={`px-4 py-2.5 text-xs font-bold border-b-2 transition flex items-center space-x-2 whitespace-nowrap ${
                activeTab === 'all-incidents'
                  ? 'border-[#174f4a] text-[#174f4a]'
                  : 'border-transparent text-[#66716E] hover:text-[#18201F]'
              }`}
            >
              <FileText size={14} />
              <span>All Incidents ({filteredIncidents.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('integrity')}
              className={`px-4 py-2.5 text-xs font-bold border-b-2 transition flex items-center space-x-2 whitespace-nowrap ${
                activeTab === 'integrity'
                  ? 'border-[#3F7D5A] text-[#3F7D5A]'
                  : 'border-transparent text-[#66716E] hover:text-[#18201F]'
              }`}
            >
              <ShieldAlert size={14} />
              <span>Integrity QA Radar ({anomalies.length})</span>
            </button>
          </div>

          <span className="text-[11px] text-[#66716E] font-mono hidden xl:inline">
            Showing verified cases under municipal SLA standard
          </span>
        </div>

        {/* TAB: Inter-Department Jurisdiction Matrix */}
        {activeTab === 'jurisdiction-matrix' && (
          <DepartmentRoutingMatrix 
            incidents={incidents}
            onSelectIncident={(inc) => navigate(`/incidents/${inc.id}`)}
          />
        )}

        {/* TAB: Duplicate Clusters & Underserved Equity Radar */}
        {activeTab === 'duplicate-equity' && (
          <DuplicateEquityRadar 
            signals={intelSignals}
            wards={wardRisks}
          />
        )}

        {/* TAB 1: Action Recommendations Queue */}
        {activeTab === 'recommendations' && (
          <Card className="space-y-4 border-[#C58A32]/40 bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div>
                <h3 className="text-sm font-bold text-[#18201F] uppercase tracking-wider flex items-center space-x-2">
                  <Zap className="w-4 h-4 text-[#C58A32]" />
                  <span>Decision Support — Actionable Recommendations</span>
                </h3>
                <p className="text-xs text-[#66716E] mt-0.5">
                  Prioritized cases with algorithm-suggested actions requiring officer decision recording.
                </p>
              </div>
              <Badge variant="warning" className="font-mono text-[10px]">
                {criticalActionCount} CRITICAL / REOPEN
              </Badge>
            </div>

            {!actionQueue || !actionQueue.recommendations || actionQueue.recommendations.length === 0 ? (
              <div className="text-center py-8 text-xs text-[#66716E]">
                No pending action recommendations requiring immediate officer decision.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
                {actionQueue.recommendations.map((rec) => {
                  const isUrgent = rec.priorityLevel === 'CRITICAL' || rec.recommendedAction === 'REOPEN_CASE' || rec.recommendedAction === 'FIELD_INSPECTION';
                  return (
                    <div key={rec.id} className={`p-4 bg-white rounded-lg border space-y-3 flex flex-col justify-between shadow-xs ${
                      isUrgent ? 'border-[#b94d36]' : 'border-[#D9DEDB]'
                    }`}>
                      <div className="space-y-2">
                        <div className="flex items-start justify-between">
                          <Badge variant={isUrgent ? 'critical' : 'warning'} className="text-[10px] uppercase font-bold">
                            {rec.recommendedAction.replace(/_/g, ' ')}
                          </Badge>
                          <span className="text-[10px] font-mono text-[#66716E]">
                            {rec.ageHours}h open • {rec.slaState}
                          </span>
                        </div>

                        <div>
                          <div className="text-[10px] font-mono text-[#174f4a] font-bold">
                            CASE #{rec.incidentId} • {rec.category}
                          </div>
                          <h4 className="text-xs font-bold text-[#18201F] leading-snug mt-0.5">
                            {rec.incidentTitle}
                          </h4>
                          <p className="text-xs text-[#66716E] mt-1 leading-relaxed">
                            {rec.reason}
                          </p>
                        </div>

                        <div className="bg-[#F5F4EF] p-2.5 rounded text-[11px] space-y-1 border border-[#D9DEDB]">
                          <div className="text-[#66716E]">
                            Suggested Dept: <strong className="text-[#18201F]">{rec.suggestedDepartment}</strong>
                          </div>
                          <div className="text-[#c58a32]">
                            Suggested Step: <strong className="text-[#18201F]">{rec.suggestedNextStep}</strong>
                          </div>
                        </div>
                      </div>

                      <div className="pt-2 border-t border-[#D9DEDB] flex items-center justify-between gap-2">
                        <button
                          onClick={() => navigate(`/incidents/${rec.incidentId}`)}
                          disabled={isOfflineDemo}
                          className={`text-xs font-semibold flex items-center space-x-1 ${
                            isOfflineDemo ? 'text-[#9aa5a1] cursor-not-allowed' : 'text-[#174f4a] hover:text-[#103c38]'
                          }`}
                        >
                          <span>{isOfflineDemo ? 'Demo case' : 'Review Case File'}</span>
                          <ArrowRight className="w-3.5 h-3.5" />
                        </button>

                        <button
                          onClick={() => {
                            setSelectedRec(rec);
                            setDecisionType('ACCEPT');
                            setDecisionReason('');
                          }}
                          disabled={isOfflineDemo}
                          className="px-2.5 py-1.5 bg-[#174f4a] hover:bg-[#123e3a] text-white font-bold text-xs rounded transition shadow-sm disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          {isOfflineDemo ? 'Live data required' : 'Record Decision'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        )}

        {/* TAB 2: All Open Incidents Queue with Comprehensive Filters */}
        {activeTab === 'all-incidents' && (
          <Card className="space-y-4 border-[#D9DEDB] bg-white shadow-sm">
            {/* Filter & Search Bar */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2 flex-1">
                <div className="relative flex-1 max-w-sm">
                  <Search className="w-3.5 h-3.5 text-[#66716E] absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    placeholder="Search by ID, title, keyword or address..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-9 pr-3 py-1.5 bg-white border border-[#D9DEDB] rounded text-xs text-[#18201F] focus:outline-none focus:border-[#174f4a]"
                  />
                  {searchQuery && (
                    <button 
                      onClick={() => setSearchQuery('')}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-[#66716E]"
                    >
                      <X size={12} />
                    </button>
                  )}
                </div>

                {/* Category Dropdown */}
                <select
                  value={categoryFilter}
                  onChange={(e) => setCategoryFilter(e.target.value)}
                  className="px-2.5 py-1.5 bg-white border border-[#D9DEDB] rounded text-xs text-[#18201F] focus:outline-none"
                  aria-label="Filter by Category"
                >
                  <option value="ALL">All Categories</option>
                  <option value="DRAINAGE">Drainage</option>
                  <option value="POTHOLE">Pothole / Road</option>
                  <option value="STREETLIGHT">Streetlight</option>
                  <option value="GARBAGE">Garbage / Sanitation</option>
                  <option value="WATER_SUPPLY">Water Supply</option>
                  <option value="OTHER">Other</option>
                </select>

                {/* Severity Dropdown */}
                <select
                  value={severityFilter}
                  onChange={(e) => setSeverityFilter(e.target.value)}
                  className="px-2.5 py-1.5 bg-white border border-[#D9DEDB] rounded text-xs text-[#18201F] focus:outline-none"
                  aria-label="Filter by Severity"
                >
                  <option value="ALL">All Severities</option>
                  <option value="CRITICAL">Critical</option>
                  <option value="HIGH">High</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="LOW">Low</option>
                </select>

                {/* Status Dropdown */}
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="px-2.5 py-1.5 bg-white border border-[#D9DEDB] rounded text-xs text-[#18201F] focus:outline-none"
                  aria-label="Filter by Status"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="REPORTED">Reported</option>
                  <option value="VERIFIED">Verified / Assigned</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="RESOLVED">Resolved</option>
                </select>
              </div>

              <div className="text-xs font-mono text-[#66716E]">
                Showing {filteredIncidents.length} of {incidents.length} cases
              </div>
            </div>

            {/* Incidents Table / Grid */}
            {incidents.length === 0 ? (
              <div className="border border-dashed border-[#D9DEDB] rounded-lg p-8 text-center space-y-2 bg-white/60">
                <CheckCircle2 className="w-8 h-8 text-[#17483B] mx-auto" />
                <h4 className="text-sm font-bold text-[#18201F]">No Municipal Incidents Recorded</h4>
                <p className="text-xs text-[#66716E] max-w-md mx-auto">
                  There are currently no civic complaints registered in the municipal database. The system is operating normally and awaiting resident submissions or field dispatches.
                </p>
              </div>
            ) : filteredIncidents.length === 0 ? (
              <div className="text-center py-8 text-xs text-[#66716E] space-y-2">
                <div>No open incidents match your search or filter criteria ({incidents.length} total cases in database).</div>
                <button
                  onClick={() => {
                    setCategoryFilter('ALL');
                    setSeverityFilter('ALL');
                    setStatusFilter('ALL');
                    setSearchQuery('');
                  }}
                  className="text-xs font-bold text-[#174f4a] hover:underline"
                >
                  Reset all filters
                </button>
              </div>
            ) : (
              <div className="space-y-2.5">
                {filteredIncidents.map((inc) => (
                  <div
                    key={inc.id}
                    className="p-3.5 bg-white border border-[#D9DEDB] hover:border-[#174f4a] rounded-lg flex flex-col md:flex-row md:items-center justify-between gap-3 transition"
                  >
                    <div className="space-y-1 flex-1">
                      <div className="flex items-center space-x-2">
                        <span className="font-mono text-xs font-bold text-[#174f4a]">CASE #{inc.id}</span>
                        <Badge variant={inc.status === 'RESOLVED' || inc.status === 'VERIFIED' ? 'success' : 'primary'}>
                          {inc.status}
                        </Badge>
                        <span className="px-2 py-0.5 rounded bg-[#F5F4EF] text-[#18201F] font-mono text-[10px] font-bold border border-[#D9DEDB]">
                          {inc.category}
                        </span>
                        <Badge variant={inc.severity === 'HIGH' || inc.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                          {inc.severity}
                        </Badge>
                        {inc.classificationTag === 'SIMULATED' && (
                          <span className="text-[10px] font-mono px-1.5 py-0.5 bg-purple-100 text-purple-800 rounded border border-purple-300 font-bold">
                            SIMULATED DEMO
                          </span>
                        )}
                        {inc.originalLanguage && inc.originalLanguage !== 'EN' && (
                          <span className="text-[10px] font-mono px-1.5 py-0.5 bg-blue-50 text-blue-700 rounded border border-blue-200">
                            LANG: {inc.originalLanguage}
                          </span>
                        )}
                      </div>
                      <h4 className="font-bold text-xs text-[#18201F]">{inc.title}</h4>
                      <p className="text-xs text-[#66716E] line-clamp-1">
                        {inc.address} • {inc.originalDescription || inc.description}
                      </p>
                      {inc.nearestRoadName && (
                        <div className="flex items-center gap-1.5 text-[10px] font-mono text-[#17483B] pt-0.5">
                          <span className="px-1.5 py-0.2 rounded bg-emerald-50 border border-emerald-200 text-emerald-800">
                            OSM Ref: {inc.nearestRoadName} ({Math.round(inc.nearestRoadDistanceMeters)}m)
                          </span>
                        </div>
                      )}
                    </div>

                    <div className="flex items-center space-x-3 shrink-0">
                      <div className="text-right text-[11px] font-mono text-[#66716E]">
                        <div>Reported: {new Date(inc.createdAt).toLocaleDateString()}</div>
                        <div className="text-[#18201F] font-semibold">{inc.assignedAreaName || 'Ward Assigned'}</div>
                      </div>

                      <button
                        onClick={() => navigate(`/incidents/${inc.id}`)}
                        className="px-3 py-1.5 bg-[#f0f2ee] hover:bg-[#174f4a] hover:text-white text-[#174f4a] font-bold text-xs rounded transition flex items-center space-x-1"
                      >
                        <span>Case File</span>
                        <ArrowRight size={13} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        )}

        {/* TAB 3: Integrity QA Radar (Non-Accusatory Framing) */}
        {activeTab === 'integrity' && (
          <Card className="space-y-4 border-[#D9DEDB] bg-white shadow-sm">
            <div className="border-b border-[#D9DEDB] pb-3">
              <h3 className="text-sm font-bold text-[#18201F] uppercase tracking-wider flex items-center space-x-2">
                <ShieldAlert className="w-4 h-4 text-[#3F7D5A]" />
                <span>Operational Integrity & Quality Assurance Review</span>
              </h3>
              <p className="text-xs text-[#66716E] mt-0.5">
                Non-accusatory operational pattern detection to ensure quality assurance, prevent premature case closure, and verify photographic evidence standards.
              </p>
            </div>

            {anomalies.length === 0 ? (
              <div className="text-center py-8 text-xs text-[#66716E] space-y-1">
                <CheckCircle2 className="w-8 h-8 text-[#3F7D5A] mx-auto" />
                <div className="font-bold text-[#18201F] text-sm">System Integrity Nominal</div>
                <p>Zero unflagged pattern anomalies detected across open municipal reports.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {anomalies.map((ano) => (
                  <div key={ano.id} className="p-4 bg-white border border-[#D9DEDB] rounded-lg space-y-3">
                    <div className="flex items-start justify-between">
                      <div>
                        <span className="font-mono text-xs text-[#174f4a] font-bold">CASE #{ano.incidentId || ano.id}</span>
                        <h4 className="font-bold text-xs text-[#18201F] mt-0.5">
                          {ano.anomalyType || 'OPERATIONAL PATTERN FLAG'}
                        </h4>
                      </div>
                      <Badge variant="warning">
                        REVIEW RECOMMENDED
                      </Badge>
                    </div>

                    <p className="text-xs text-[#18201F] bg-[#F5F4EF] p-2.5 rounded border border-[#D9DEDB] leading-relaxed">
                      {ano.description || ano.reason || 'Pattern flagged for routine verification before final archival closure.'}
                    </p>

                    <div className="flex items-center justify-between text-[11px] font-mono text-[#66716E] pt-2 border-t border-[#D9DEDB]">
                      <span>Quality check routine</span>
                      <button
                        onClick={() => navigate(`/incidents/${ano.incidentId || ano.id}`)}
                        className="text-xs text-[#174f4a] hover:underline font-bold flex items-center space-x-1"
                      >
                        <span>Audit Case File</span>
                        <ArrowRight size={13} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        )}
      </div>

      {/* 5. Civic Intelligence Patterns & Closed-Loop Learning */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        
        {/* Left: Spatial Radar Pattern Signals */}
        <Card className="space-y-3.5 border-[#D9DEDB] bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2.5">
            <div>
              <h3 className="font-bold text-xs uppercase tracking-wider text-[#18201F] flex items-center space-x-1.5">
                <TrendingUp className="w-4 h-4 text-[#C87941]" />
                <span>Spatial Pattern Radar ({intelSignals.length})</span>
              </h3>
              <p className="text-xs text-[#66716E]">
                Emerging trends and spatial concentrations detected by 500m clustering.
              </p>
            </div>
            <Badge variant="purple" className="font-mono text-[10px]">RADAR-v1</Badge>
          </div>

          {intelSignals.length === 0 ? (
            <div className="text-center py-6 text-xs text-[#66716E]">
              No active spatial pattern concentrations detected at current threshold.
            </div>
          ) : (
            <div className="space-y-2.5">
              {intelSignals.map((sig) => (
                <div key={sig.id} className="p-3 bg-[#F5F4EF] rounded border border-[#D9DEDB] space-y-1.5">
                  <div className="flex items-center justify-between">
                    <Badge variant={sig.signalType === 'EMERGING_ISSUE' ? 'critical' : 'purple'}>
                      {sig.signalType}
                    </Badge>
                    <span className="text-[10px] font-mono text-[#66716E]">{sig.timeWindow}</span>
                  </div>
                  <h4 className="text-xs font-bold text-[#18201F]">{sig.category} • {sig.areaName}</h4>
                  <p className="text-xs text-[#556360] leading-relaxed">{sig.explanation}</p>
                  <div className="flex justify-between text-[11px] font-mono pt-1 text-[#66716E]">
                    <span>Incident Count: <strong className="text-[#18201F]">{sig.incidentCount}</strong></span>
                    <span>Unresolved: <strong className="text-[#b94d36]">{sig.unresolvedCount}</strong></span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* Right: Closed-Loop Operational Learning Signals */}
        <Card className="space-y-3.5 border-[#D9DEDB] bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2.5">
            <div>
              <h3 className="font-bold text-xs uppercase tracking-wider text-[#18201F] flex items-center space-x-1.5">
                <BookOpen className="w-4 h-4 text-[#174f4a]" />
                <span>Closed-Loop Operational Learning ({learningSignals.length})</span>
              </h3>
              <p className="text-xs text-[#66716E]">
                Systemic infrastructure insights from resolution outcomes and reopening audits.
              </p>
            </div>
            <Badge variant="cyan" className="font-mono text-[10px]">LEARNING-v1</Badge>
          </div>

          {learningSignals.length === 0 ? (
            <div className="text-center py-6 text-xs text-[#66716E]">
              No chronic operational learning alerts currently triggered.
            </div>
          ) : (
            <div className="space-y-2.5">
              {learningSignals.map((ls) => (
                <div key={ls.id} className="p-3 bg-[#F5F4EF] rounded border border-[#D9DEDB] space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-[10px] text-[#174f4a] font-bold uppercase">
                      {ls.signalType.replace(/_/g, ' ')}
                    </span>
                    <span className="text-[10px] font-mono text-[#66716E]">{ls.sampleCount} cases</span>
                  </div>
                  <h4 className="text-xs font-bold text-[#18201F]">{ls.category} • {ls.areaName}</h4>
                  <p className="text-xs text-[#556360] leading-relaxed">{ls.observation}</p>
                  <div className="bg-white p-2 rounded border border-[#D9DEDB] text-[11px]">
                    <span className="font-bold text-[#174f4a] block text-[10px] uppercase">Preventative Strategy:</span>
                    <p className="text-[#18201F] mt-0.5">{ls.recommendedStrategy}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {/* 6. Officer Decision Recording Modal */}
      {selectedRec && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white border border-[#D9DEDB] rounded-lg max-w-lg w-full p-5 space-y-4 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <Zap className="w-5 h-5 text-[#b94d36]" />
                <h3 className="text-base font-bold text-[#18201F]">Record Municipal Officer Decision</h3>
              </div>
              <button 
                onClick={() => setSelectedRec(null)} 
                className="text-[#66716E] hover:text-[#18201F] font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <div className="bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] space-y-1 text-xs font-mono">
              <div>
                <span className="text-[#66716E]">Incident:</span>{' '}
                <strong className="text-[#18201F]">#{selectedRec.incidentId} • {selectedRec.incidentTitle}</strong>
              </div>
              <div>
                <span className="text-[#66716E]">Recommended Action:</span>{' '}
                <strong className="text-[#b94d36]">{selectedRec.recommendedAction}</strong>
              </div>
              <div>
                <span className="text-[#66716E]">Reasoning:</span>{' '}
                <span className="text-[#18201F]">{selectedRec.reason}</span>
              </div>
            </div>

            <div className="space-y-3 text-xs">
              <label className="block font-bold text-[#18201F] uppercase tracking-wider text-[10px]">
                Officer Decision:
              </label>
              <div className="grid grid-cols-4 gap-2">
                {['ACCEPT', 'MODIFY', 'DEFER', 'REJECT'].map((dt) => (
                  <button
                    key={dt}
                    onClick={() => setDecisionType(dt)}
                    className={`py-2 text-xs font-bold rounded border transition ${
                      decisionType === dt
                        ? 'bg-[#174f4a] border-[#174f4a] text-white'
                        : 'bg-white border-[#D9DEDB] text-[#18201F] hover:bg-[#F5F4EF]'
                    }`}
                  >
                    {dt}
                  </button>
                ))}
              </div>

              {decisionType !== 'ACCEPT' && (
                <div className="space-y-1">
                  <label className="block font-bold text-[#b94d36] text-[11px]">
                    Decision Operational Rationale (Required):
                  </label>
                  <textarea
                    rows={3}
                    value={decisionReason}
                    onChange={(e) => setDecisionReason(e.target.value)}
                    placeholder="Provide specific operational justification for modifying, deferring, or rejecting this recommendation..."
                    className="w-full bg-white border border-[#D9DEDB] rounded p-2.5 text-xs text-[#18201F] focus:outline-none focus:border-[#174f4a]"
                  />
                </div>
              )}
            </div>

            <div className="pt-2 border-t border-[#D9DEDB] flex justify-end space-x-2">
              <button
                onClick={() => setSelectedRec(null)}
                className="px-3 py-1.5 bg-[#F5F4EF] hover:bg-[#e0ded8] text-[#18201F] text-xs font-bold rounded"
              >
                Cancel
              </button>
              <button
                onClick={handleRecordDecision}
                disabled={decisionSubmitting}
                className="px-3 py-1.5 bg-[#174f4a] hover:bg-[#123e3a] text-white text-xs font-bold rounded transition disabled:opacity-50"
              >
                {decisionSubmitting ? 'Recording...' : 'Submit Officer Decision'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 7. Data Provenance & Source Registry Modal */}
      {showProvenanceModal && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white border border-[#D9DEDB] rounded-lg max-w-2xl w-full p-5 space-y-4 shadow-2xl relative max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <Database className="w-5 h-5 text-[#b94d36]" />
                <h3 className="text-base font-bold text-[#18201F]">Nirvaha Data Provenance & Source Registry</h3>
              </div>
              <button 
                onClick={() => setShowProvenanceModal(false)} 
                className="text-[#66716E] hover:text-[#18201F] font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <div className="space-y-3">
              {sources.map((src) => (
                <div key={src.id} className="bg-[#F5F4EF] p-3.5 rounded border border-[#D9DEDB] space-y-2">
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="text-xs font-bold text-[#18201F]">{src.name}</h4>
                      <p className="text-xs text-[#66716E] mt-0.5">{src.publisher}</p>
                    </div>
                    <Badge variant="cyan">{src.classification || 'DERIVED_FROM_OFFICIAL'}</Badge>
                  </div>
                  <p className="text-xs text-[#18201F] leading-relaxed">{src.description}</p>
                  <div className="flex items-center justify-between pt-1 border-t border-[#D9DEDB] text-[10px] font-mono text-[#66716E]">
                    <span>License: {src.license}</span>
                    {src.catalogUrl && (
                      <a href={src.catalogUrl} target="_blank" rel="noreferrer" className="text-[#174f4a] hover:underline flex items-center space-x-1">
                        <span>Source Catalog</span>
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 8. Live Integration Health & Source Verification Modal */}
      {showIntegrationModal && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white border border-[#D9DEDB] rounded-lg max-w-3xl w-full p-5 space-y-4 shadow-2xl relative max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <Activity className="w-5 h-5 text-[#17483B]" />
                <div>
                  <h3 className="text-base font-bold text-[#18201F]">Territorial Data Integrations & Operational Health</h3>
                  <p className="text-xs text-[#66716E]">Puducherry UT Authoritative Source Verification & Live Telemetry</p>
                </div>
              </div>
              <div className="flex items-center space-x-2">
                <Badge variant={integrationHealth?.systemStatus === 'CONNECTED' || integrationHealth?.systemStatus === 'HEALTHY' ? 'success' : 'warning'}>
                  System: {integrationHealth?.systemStatus || 'CHECKING'}
                </Badge>
                <button 
                  onClick={() => setShowIntegrationModal(false)} 
                  className="text-[#66716E] hover:text-[#18201F] font-bold text-lg p-1"
                >
                  ✕
                </button>
              </div>
            </div>

            <div className="space-y-3">
              {integrationHealth?.integrations?.map((item, idx) => (
                <div key={idx} className="bg-[#F8FAF9] p-3.5 rounded border border-[#E2E8E5] space-y-2">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h4 className="text-xs font-bold text-[#18201F] flex items-center gap-1.5">
                        <span>{item.name}</span>
                      </h4>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-[10px] font-mono text-[#66716E] bg-white px-1.5 py-0.5 rounded border border-[#E2E8E5]">
                          {item.category}
                        </span>
                        <span className="text-[10px] font-mono text-[#66716E]">
                          Auth: <strong>{item.authType}</strong>
                        </span>
                      </div>
                    </div>
                    <Badge variant={
                      item.status === 'CONNECTED' || item.status === 'HEALTHY' ? 'success' :
                      item.status === 'NOT_CONFIGURED' ? 'default' :
                      item.status === 'STALE' ? 'warning' :
                      item.status === 'PENDING_AUTHORIZATION' ? 'cyan' :
                      item.status === 'UNAVAILABLE' ? 'danger' : 'warning'
                    }>
                      {item.status}
                    </Badge>
                  </div>

                  <p className="text-xs text-[#18201F] leading-relaxed bg-white p-2 rounded border border-[#E2E8E5]/60 font-sans">
                    {item.observationSummary}
                  </p>

                  <div className="flex flex-wrap items-center justify-between pt-1 border-t border-[#E2E8E5] text-[10px] font-mono text-[#66716E] gap-2">
                    <span>Endpoint: <span className="text-[#18201F]">{item.endpoint}</span></span>
                    {item.errorCategory && (
                      <span className="text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded border border-amber-200">
                        Notice: {item.errorCategory}
                      </span>
                    )}
                    {item.lastObservationTimestamp && (
                      <span>Last Seen: {new Date(item.lastObservationTimestamp).toLocaleTimeString()}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <div className="pt-2 border-t border-[#D9DEDB] flex justify-between items-center text-xs text-[#66716E]">
              <span>Evaluated: {integrationHealth?.evaluatedAt ? new Date(integrationHealth.evaluatedAt).toLocaleString() : 'Just now'}</span>
              <button
                onClick={fetchData}
                className="px-3 py-1.5 bg-[#17483B] hover:bg-[#102D27] text-white font-bold rounded flex items-center space-x-1.5 transition text-xs"
              >
                <RefreshCcw className="w-3 h-3" />
                <span>Re-check Integrations</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Judge Demonstration Mode Modal */}
      <JudgeDemoModal
        isOpen={showJudgeDemoModal}
        onClose={() => setShowJudgeDemoModal(false)}
        onRefreshData={(simFlag) => {
          if (simFlag !== undefined) setIncludeSimulated(simFlag);
          fetchData(simFlag !== undefined ? simFlag : includeSimulated);
        }}
      />

      </main>
    </div>
  );
};
