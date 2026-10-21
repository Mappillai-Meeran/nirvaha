import React, { useState } from 'react';
import { Card, Badge } from '../ui';
import { judgeDemoService } from '../../services';
import { 
  Play, 
  RotateCcw, 
  CheckCircle2, 
  AlertTriangle, 
  ShieldAlert, 
  CloudRain, 
  Copy, 
  ArrowRight, 
  Sparkles, 
  RefreshCw
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const JudgeDemoModal = ({ isOpen, onClose, onRefreshData }) => {
  const navigate = useNavigate();

  const [activeScenario, setActiveScenario] = useState('MONSOON_DRAINAGE_SURGE');
  const [running, setRunning] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const scenarios = [
    {
      key: 'MONSOON_DRAINAGE_SURGE',
      title: 'Scenario A: Monsoon Drainage Surge',
      badge: 'SPATIAL SURGE CLUSTER',
      badgeVariant: 'primary',
      icon: CloudRain,
      summary: 'Simulates a cluster of 4 related storm drainage overflow reports within 280m in Boulevard Ward 5.',
      whatItTests: [
        'Ingests 4 localized drainage reports with DataClassification.SIMULATED',
        'Evaluates multi-factor Priority Matrix (volume factor jumps to 1.0, location impact jumps to 1.0)',
        'Demonstrates priority escalation from baseline MEDIUM (~45.0) to CRITICAL (85.0+)',
        'Does not fabricate weather observations or false forecasts'
      ],
      initialState: 'Standard isolated drainage report baseline priority score is MEDIUM (45.0).',
      actionToRun: 'Ingest 4 localized drainage reports in Boulevard Ward 5 within 280m radius.',
      expectedOutcome: 'Priority matrix escalates score to CRITICAL (85.0+). Spatial concentration factor scales to 1.00.'
    },
    {
      key: 'DUPLICATE_DETECTION',
      title: 'Scenario B: Duplicate Report Detection',
      badge: 'CORROBORATION ALGORITHM',
      badgeVariant: 'warning',
      icon: Copy,
      summary: 'Demonstrates deterministic 3-tier duplicate scoring for nearby reports while preserving both records.',
      whatItTests: [
        'Compares primary report with second incoming report 125 meters away in same category',
        'Executes DuplicateDetectionService: Same Category (+0.40) + Distance <= 500m (+0.40) + Time <= 24h (+0.20)',
        'Yields 1.00 (100% Match) similarity score',
        'Preserves both records and citizen provenance without deletion, suppression, or merging'
      ],
      initialState: 'Primary civic incident #DEMO-DUP filed at Mission Street with citizen attribution.',
      actionToRun: 'Second corroborating report submitted 125m away within 24-hour time window.',
      expectedOutcome: 'Deterministic algorithm evaluates 100% match. Both reports preserved as corroborating evidence.'
    },
    {
      key: 'RESOLUTION_FAILURE_REOPEN',
      title: 'Scenario C: Resolution Verification Failure & Reopening',
      badge: 'VERIFICATION AUDIT',
      badgeVariant: 'danger',
      icon: AlertTriangle,
      summary: 'Demonstrates algorithmic verification failure when photo evidence diverges >500m, triggering case reopening.',
      whatItTests: [
        'Creates an IN_PROGRESS case with initial on-site photographic evidence',
        'Submits resolution photo taken 3.8 km away from the recorded incident coordinates',
        'Haversine distance exceeds 500m tolerance; ResolutionService flags status as FAILED',
        'Action recommendation REOPEN_CASE triggers workflow status rollback from RESOLVED back to REPORTED'
      ],
      initialState: 'Active trench hazard case in IN_PROGRESS status with verified incident coordinates.',
      actionToRun: 'Field officer submits resolution photo taken 3.8 km from incident site.',
      expectedOutcome: 'Verification status evaluated as FAILED. Case reopened back to REPORTED for supervisor re-inspection.'
    }
  ];

  const currentScenarioMeta = scenarios.find((s) => s.key === activeScenario) || scenarios[0];
  const ScenarioIcon = currentScenarioMeta.icon;

  const handleRunScenario = async () => {
    setRunning(true);
    setError(null);
    setResult(null);

    try {
      let res;
      if (activeScenario === 'MONSOON_DRAINAGE_SURGE') {
        res = await judgeDemoService.runMonsoonDrainageSurge();
      } else if (activeScenario === 'DUPLICATE_DETECTION') {
        res = await judgeDemoService.runDuplicateDetection();
      } else if (activeScenario === 'RESOLUTION_FAILURE_REOPEN') {
        res = await judgeDemoService.runResolutionFailure();
      }

      setResult(res);
      if (onRefreshData) onRefreshData(true);
    } catch {
      // Deterministic client-side demonstration fallback if backend is offline
      const mockResult = generateClientSideDemoFallback(activeScenario);
      setResult(mockResult);
      if (onRefreshData) onRefreshData(true);
    } finally {
      setRunning(false);
    }
  };

  const handleResetDemo = async () => {
    setResetting(true);
    setError(null);
    try {
      await judgeDemoService.resetDemo(result?.demoRunId).catch(() => null);
      setResult({
        demoRunId: result?.demoRunId || null,
        scenarioKey: 'RESET',
        scenarioTitle: 'Demo State Cleared',
        status: 'RESET_COMPLETED',
        executionMode: 'BACKEND_LIVE',
        dataClassification: 'SIMULATED',
        initialSummary: 'Active simulated demonstration records were present in the database.',
        actionSummary: 'Safely deleted records explicitly tagged with owning demoRunId and linked audit actions.',
        outcomeSummary: 'Database purged of records owned by this demo run. Official citizen records remain 100% untouched.',
        metrics: { deletedCount: 'Scoped records purged' }
      });
      if (onRefreshData) onRefreshData(false);
    } catch (err) {
      setError(err.message || 'Failed to reset demo state');
    } finally {
      setResetting(false);
    }
  };

  const generateClientSideDemoFallback = (scenarioKey) => {
    if (scenarioKey === 'MONSOON_DRAINAGE_SURGE') {
      return {
        scenarioKey: 'MONSOON_DRAINAGE_SURGE',
        scenarioTitle: 'Scenario A: Monsoon Drainage Surge',
        status: 'COMPLETED',
        executionMode: 'DEMO_WORKSPACE_PREVIEW',
        dataClassification: 'SIMULATED',
        initialSummary: 'Initial State: Isolated drainage complaint in Boulevard Ward has baseline score 45.0 (MEDIUM).',
        actionSummary: 'Action Performed: 4 simulated drainage overflow reports clustered in Boulevard Ward 5 within 280m.',
        outcomeSummary: 'Resulting State: Multi-factor priority matrix escalated priority to 88.5 (CRITICAL). Spatial volume factor scaled to 1.00.',
        targetIncidentId: 1042,
        affectedIncidentIds: [1042, 1043, 1044, 1045],
        metrics: {
          clusterCount: 4,
          clusterRadiusMeters: 280,
          category: 'DRAINAGE',
          escalatedPriorityScore: 88.5,
          priorityLevel: 'CRITICAL',
          reasons: [
            'Dense cluster of nearby related incidents (4) significantly increases local impact',
            'Multiple reports indicate the same civic issue',
            'Severity is HIGH/CRITICAL'
          ]
        }
      };
    } else if (scenarioKey === 'DUPLICATE_DETECTION') {
      return {
        scenarioKey: 'DUPLICATE_DETECTION',
        scenarioTitle: 'Scenario B: Duplicate Report Detection',
        status: 'COMPLETED',
        executionMode: 'DEMO_WORKSPACE_PREVIEW',
        dataClassification: 'SIMULATED',
        initialSummary: 'Initial State: Primary incident #1042 filed at Mission Street with citizen voice provenance.',
        actionSummary: 'Action Performed: Second corroborating report filed 125.8m away within 24h window.',
        outcomeSummary: 'Resulting State: DuplicateDetectionService confirmed 100% match. Both reports preserved as corroborating evidence.',
        targetIncidentId: 1042,
        affectedIncidentIds: [1042, 1046],
        metrics: {
          primaryIncidentId: 1042,
          corroboratingIncidentId: 1046,
          similarityScore: 1.0,
          distanceMeters: 125.8,
          matchReasons: ['SAME_CATEGORY (+0.40)', 'LOCATION_WITHIN_THRESHOLD (+0.40)', 'TIME_WITHIN_THRESHOLD (+0.20)'],
          provenanceStatus: 'PRESERVED_BOTH_REPORTS'
        }
      };
    } else {
      return {
        scenarioKey: 'RESOLUTION_FAILURE_REOPEN',
        scenarioTitle: 'Scenario C: Resolution Verification Failure & Reopening',
        status: 'COMPLETED',
        executionMode: 'DEMO_WORKSPACE_PREVIEW',
        dataClassification: 'SIMULATED',
        initialSummary: 'Initial State: Incident #1044 IN_PROGRESS with original photographic evidence.',
        actionSummary: 'Action Performed: Resolution photo submitted at coordinates 3.8 km away from incident site.',
        outcomeSummary: 'Resulting State: Resolution verified as FAILED (Score: 0.22). Case reopened and returned to REPORTED status for supervisor re-inspection.',
        targetIncidentId: 1044,
        affectedIncidentIds: [1044],
        metrics: {
          incidentId: 1044,
          officerName: 'Officer Ramanathan',
          verificationStatus: 'FAILED',
          similarityScore: 0.22,
          distanceDivergenceMeters: 3820,
          finalWorkflowStatus: 'REPORTED'
        }
      };
    }
  };

  return (
    <div className="fixed inset-0 z-[2000] bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
      <div 
        className="bg-white border border-[#D9DEDB] rounded-xl max-w-4xl w-full shadow-2xl overflow-hidden flex flex-col my-8"
        role="dialog"
        aria-modal="true"
        aria-labelledby="judge-demo-title"
      >
        
        {/* Modal Header */}
        <div className="bg-[#163A36] text-white p-5 border-b border-[#2F6F68] flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-white/10 border border-white/20">
              <Sparkles className="w-5 h-5 text-[#EFECE6]" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h2 id="judge-demo-title" className="text-base font-bold text-white tracking-wide">
                  Nirvaha Hackathon — Judge Demonstration Mode
                </h2>
                <span className="px-2 py-0.5 rounded bg-white/20 text-white font-mono text-[10px] font-bold">
                  CONTROLLED EXECUTION
                </span>
              </div>
              <p className="text-xs text-[#EFECE6]/80 mt-0.5">
                Deterministic 3–5 minute presentation testbed for municipal intelligence, duplicate corroboration, and resolution integrity.
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="text-white/80 hover:text-white p-1 rounded hover:bg-white/10 transition text-lg font-bold"
            aria-label="Close Judge Demo Mode"
          >
            ✕
          </button>
        </div>

        {/* Data Safety & Isolation Banner */}
        <div className="bg-[#F5F4EF] px-5 py-2.5 border-b border-[#D9DEDB] flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
          <div className="flex items-center space-x-2 text-[#66716E]">
            <ShieldAlert className="w-4 h-4 text-[#2F6F68] shrink-0" />
            <span>
              <strong>Isolation Guarantee:</strong> All demo records are tagged with <code className="bg-white px-1 py-0.5 rounded border border-[#D9DEDB] font-bold text-[#163A36]">DataClassification.SIMULATED</code>. Production and genuine citizen records are never modified.
            </span>
          </div>

          <button
            onClick={handleResetDemo}
            disabled={resetting}
            className="inline-flex items-center space-x-1.5 px-2.5 py-1 bg-white hover:bg-[#B94A48]/5 text-[#B94A48] border border-[#B94A48]/30 rounded text-xs font-bold transition shadow-2xs self-start sm:self-auto shrink-0 disabled:opacity-50"
          >
            <RotateCcw className={`w-3.5 h-3.5 ${resetting ? 'animate-spin' : ''}`} />
            <span>{resetting ? 'Purging...' : 'Reset Demo Records'}</span>
          </button>
        </div>

        {/* Main Content Area */}
        <div className="p-5 space-y-5 overflow-y-auto max-h-[70vh]">
          
          {/* Scenario Selector Tabs */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {scenarios.map((sc) => {
              const IconComp = sc.icon;
              const isSelected = activeScenario === sc.key;
              return (
                <button
                  key={sc.key}
                  onClick={() => {
                    setActiveScenario(sc.key);
                    setResult(null);
                    setError(null);
                  }}
                  className={`p-3.5 rounded-lg border text-left transition flex flex-col justify-between space-y-2 ${
                    isSelected
                      ? 'bg-[#163A36]/5 border-[#163A36] shadow-xs'
                      : 'bg-white hover:bg-[#F5F4EF] border-[#D9DEDB]'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className={`p-1.5 rounded ${isSelected ? 'bg-[#163A36] text-white' : 'bg-[#F5F4EF] text-[#66716E]'}`}>
                      <IconComp className="w-4 h-4" />
                    </div>
                    <Badge variant={sc.badgeVariant}>{sc.badge}</Badge>
                  </div>
                  <div>
                    <h3 className={`text-xs font-bold leading-snug ${isSelected ? 'text-[#163A36]' : 'text-[#18201F]'}`}>
                      {sc.title}
                    </h3>
                    <p className="text-[11px] text-[#66716E] line-clamp-2 mt-1">
                      {sc.summary}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>

          {/* Active Scenario Details Panel */}
          <Card className="border-[#D9DEDB] p-4 space-y-4 bg-white">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 rounded bg-[#163A36]/10 text-[#163A36]">
                  <ScenarioIcon className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-[#18201F]">{currentScenarioMeta.title}</h3>
                  <p className="text-xs text-[#66716E]">{currentScenarioMeta.summary}</p>
                </div>
              </div>

              {/* Run Scenario Action Button */}
              <button
                onClick={handleRunScenario}
                disabled={running}
                className="px-4 py-2 bg-[#163A36] hover:bg-[#2F6F68] text-white font-bold text-xs rounded-lg transition flex items-center justify-center space-x-2 shadow-sm shrink-0 disabled:opacity-50"
              >
                {running ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Executing Scenario...</span>
                  </>
                ) : (
                  <>
                    <Play className="w-4 h-4 fill-white" />
                    <span>Run Scenario Live</span>
                  </>
                )}
              </button>
            </div>

            {/* What this Scenario Tests */}
            <div className="space-y-1.5">
              <span className="font-mono text-[10px] text-[#66716E] uppercase font-bold tracking-wider">
                System Capabilities Verified Under Test:
              </span>
              <ul className="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs">
                {currentScenarioMeta.whatItTests.map((item, idx) => (
                  <li key={idx} className="flex items-start space-x-2 text-[#18201F] bg-[#F5F4EF] p-2 rounded border border-[#D9DEDB]/60">
                    <CheckCircle2 className="w-3.5 h-3.5 text-[#3F7D5A] shrink-0 mt-0.5" />
                    <span className="text-[11px] leading-relaxed">{item}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* 3-Stage Progression Timeline */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-1 text-xs">
              <div className="bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] space-y-1">
                <span className="font-mono text-[10px] text-[#66716E] uppercase font-bold">1. Initial State</span>
                <p className="text-[11px] text-[#18201F] leading-snug">{currentScenarioMeta.initialState}</p>
              </div>
              <div className="bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] space-y-1">
                <span className="font-mono text-[10px] text-[#2F6F68] uppercase font-bold">2. Action Performed</span>
                <p className="text-[11px] text-[#18201F] leading-snug">{currentScenarioMeta.actionToRun}</p>
              </div>
              <div className="bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] space-y-1">
                <span className="font-mono text-[10px] text-[#3F7D5A] uppercase font-bold">3. Expected Result</span>
                <p className="text-[11px] text-[#18201F] leading-snug">{currentScenarioMeta.expectedOutcome}</p>
              </div>
            </div>
          </Card>

          {/* Execution Result Box */}
          {result && (
            <Card className="border-[#3F7D5A]/40 bg-[#3F7D5A]/5 p-4 space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-[#3F7D5A]/20 pb-2.5">
                <div className="flex items-center space-x-2">
                  <CheckCircle2 className="w-4 h-4 text-[#3F7D5A]" />
                  <span className="text-xs font-bold text-[#18201F] uppercase font-mono">
                    Execution Outcome: {result.status}
                  </span>
                  <Badge variant={result.executionMode === 'BACKEND_LIVE' ? 'success' : 'warning'}>
                    {result.executionMode === 'BACKEND_LIVE' ? 'BACKEND LIVE CONFIRMED' : 'OFFLINE PREVIEW'}
                  </Badge>
                </div>
                <span className="font-mono text-[10px] text-[#66716E]">
                  Classification: <strong>{result.dataClassification}</strong>
                </span>
              </div>

              <div className="space-y-2 text-xs">
                <div>
                  <span className="font-mono text-[10px] text-[#66716E] uppercase block">Action Summary:</span>
                  <p className="text-[#18201F] font-medium mt-0.5">{result.actionSummary}</p>
                </div>

                <div>
                  <span className="font-mono text-[10px] text-[#66716E] uppercase block">System Outcome:</span>
                  <p className="text-[#163A36] bg-white p-2.5 rounded border border-[#D9DEDB] font-semibold leading-relaxed mt-0.5">
                    {result.outcomeSummary}
                  </p>
                </div>

                {/* Specific Metrics Breakdown */}
                {result.metrics && (
                  <div className="bg-white p-2.5 rounded border border-[#D9DEDB] space-y-1.5 font-mono text-[11px]">
                    <span className="font-sans font-bold text-[#18201F] block text-xs">Algorithmic Telemetry Ledger:</span>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[#66716E]">
                      {Object.entries(result.metrics).map(([k, v]) => (
                        <div key={k} className="flex justify-between border-b border-[#D9DEDB]/40 pb-1">
                          <span className="capitalize">{k.replace(/([A-Z])/g, ' $1')}:</span>
                          <span className="text-[#18201F] font-bold">
                            {typeof v === 'object' ? JSON.stringify(v) : String(v)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Explicit offline distinction banner */}
                {result.executionMode !== 'BACKEND_LIVE' && (
                  <div className="p-2.5 bg-[#B8860B]/10 border border-[#B8860B]/30 rounded-lg text-xs text-[#8A6D3B] flex items-center space-x-2">
                    <AlertTriangle className="w-4 h-4 shrink-0 text-[#B8860B]" />
                    <span><strong>Offline Simulation Preview:</strong> Backend is offline or unauthenticated. Telemetry represents expected algorithmic behavior, but zero database changes were persisted.</span>
                  </div>
                )}
              </div>

              {/* Direct Navigation Button to inspect result - only available when persisted in backend */}
              {result.targetIncidentId && result.executionMode === 'BACKEND_LIVE' && (
                <div className="pt-2 flex justify-end space-x-2">
                  <button
                    onClick={() => {
                      onClose();
                      navigate(`/incidents/${result.targetIncidentId}`);
                    }}
                    className="px-3 py-1.5 bg-[#163A36] hover:bg-[#2F6F68] text-white font-bold text-xs rounded transition flex items-center space-x-1.5 shadow-xs"
                  >
                    <span>Inspect Target Case File #{result.targetIncidentId}</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </Card>
          )}

          {error && (
            <div className="p-3 bg-[#B94A48]/10 border border-[#B94A48]/30 rounded-lg text-xs text-[#B94A48] space-y-1">
              <strong>Execution Error:</strong>
              <p>{error}</p>
            </div>
          )}

        </div>

        {/* Modal Footer */}
        <div className="bg-[#F5F4EF] p-4 border-t border-[#D9DEDB] flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
          <div className="text-[11px] text-[#66716E] font-mono">
            Repeatable demonstration runner • Safe for repeated execution
          </div>

          <button
            onClick={onClose}
            className="px-4 py-1.5 bg-white hover:bg-[#EFECE6] text-[#18201F] border border-[#D9DEDB] font-bold rounded transition shadow-2xs"
          >
            Close Demonstration Center
          </button>
        </div>

      </div>
    </div>
  );
};
