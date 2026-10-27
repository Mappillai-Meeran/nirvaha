import React, { useEffect, useState, useCallback, useRef } from 'react';
import { 
  incidentService, 
  evidenceService, 
  duplicateService, 
  priorityService, 
  workflowService, 
  resolutionService, 
  integrityService,
  feedbackService
} from '../../services';
import { useAuth } from '../../context/AuthContext';
import { Card, Badge, LoadingState, ErrorState, FeedbackToast } from '../../components/ui';
import { ResolutionEvidenceVerification } from '../../components/evidence/ResolutionEvidenceVerification';
import { DEMO_WORKSPACE } from '../../data/demoWorkspace';
import { 
  ShieldAlert, 
  MapPin, 
  CheckCircle2, 
  AlertTriangle, 
  Clock, 
  UserCheck, 
  Camera, 
  Layers,
  ArrowLeft,
  Globe,
  RotateCcw,
  PlusCircle
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';

export const IncidentDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { currentUser } = useAuth();

  const [activeTab, setActiveTab] = useState('evidence'); // 'evidence' | 'intelligence'
  const [incident, setIncident] = useState(null);
  const [evidenceList, setEvidenceList] = useState([]);
  const [duplicates, setDuplicates] = useState([]);
  const [priority, setPriority] = useState(null);
  const [resolution, setResolution] = useState(null);
  const [anomalies, setAnomalies] = useState([]);
  const [actions, setActions] = useState([]);
  const [feedbackList, setFeedbackList] = useState([]);
  const [toast, setToast] = useState({ open: false, message: '', type: 'error' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Active request ref to prevent stale responses and unmounted state updates
  const activeRequestIdRef = useRef(id);

  // Adjust state during render when id changes to prevent stale data display without cascading effect renders
  const [currentId, setCurrentId] = useState(id);
  if (id !== currentId) {
    setCurrentId(id);
    setLoading(true);
    setError(null);
    setIncident(null);
  }

  // Modal States
  const [showResolutionModal, setShowResolutionModal] = useState(false);
  const [resolutionRemarks, setResolutionRemarks] = useState('');
  const [resolutionFile, setResolutionFile] = useState(null);
  const [submittingResolution, setSubmittingResolution] = useState(false);

  const [showReopenModal, setShowReopenModal] = useState(false);
  const [reopenRemarks, setReopenRemarks] = useState('');
  const [submittingReopen, setSubmittingReopen] = useState(false);

  const [showProgressModal, setShowProgressModal] = useState(false);
  const [progressRemarks, setProgressRemarks] = useState('');
  const [submittingProgress, setSubmittingProgress] = useState(false);

  const fetchIncidentData = useCallback(async () => {
    const fetchId = id;
    try {
      const inc = await incidentService.getIncidentById(fetchId);
      if (activeRequestIdRef.current !== fetchId) return;
      setIncident(inc);

      const [evData, dupData, prioData, resData, anoData, actData, fbData] = await Promise.all([
        evidenceService.getEvidenceForIncident(fetchId).catch(() => []),
        duplicateService.findDuplicates(fetchId).catch(() => []),
        priorityService.getPriority(fetchId).catch(() => null),
        resolutionService.getResolutionForIncident(fetchId).catch(() => null),
        integrityService.getAnomaliesForIncident(fetchId).catch(() => []),
        workflowService.getActionsForIncident(fetchId).catch(() => []),
        feedbackService.getFeedbackForIncident(fetchId).catch(() => []),
      ]);

      if (activeRequestIdRef.current !== fetchId) return;
      setEvidenceList(evData || []);
      setDuplicates(Array.isArray(dupData) ? dupData : (dupData?.matches || []));
      setPriority(prioData);
      setResolution(resData);
      setAnomalies(Array.isArray(anoData) ? anoData : (anoData?.anomalies || []));
      setActions(actData || []);
      setFeedbackList(fbData || []);
      setError(null);
    } catch (err) {
      if (activeRequestIdRef.current !== fetchId) return;
      if (err?.status === 403 || err?.status === 401) {
        setError(err.message || 'Access Denied: You do not have permission to access this incident record.');
        setIncident(null);
        return;
      }
      // Demonstration fallback if local municipal backend is offline or for demo IDs
      const demoInc = DEMO_WORKSPACE.incidents.find(
        (item) => String(item.id) === String(fetchId) || String(item.id).endsWith(String(fetchId))
      );
      if (demoInc) {
        setIncident(demoInc);
        setEvidenceList([
          {
            id: 101,
            incidentId: demoInc.id,
            evidenceType: 'PHOTO',
            fileUrl: '/demo/before_drain.jpg',
            fileName: 'before_drain.jpg',
            mimeType: 'image/jpeg',
            fileSize: 63645,
            contentHash: '93734373062566157e4b9606c3c76db2c1c5bc6214d8e2353c35e821c82f9d8a',
            latitude: demoInc.latitude,
            longitude: demoInc.longitude,
            description: demoInc.description,
            capturedAt: demoInc.createdAt,
          }
        ]);
        setResolution(
          demoInc.status === 'RESOLVED' || demoInc.status === 'VERIFIED'
            ? {
                id: 51,
                incidentId: demoInc.id,
                officerId: 1,
                officerName: 'Officer Ramanathan',
                resolutionEvidenceId: 102,
                resolutionEvidenceUrl: '/demo/after_drain.jpg',
                resolutionSimilarityScore: 0.88,
                verificationStatus: 'VERIFIED',
                remarks: 'Storm culvert dredged and masonry curb aligned. Gravity flow verified.',
                createdAt: new Date().toISOString(),
              }
            : null
        );
        setActions([
          { 
            id: 1, 
            incidentId: demoInc.id, 
            officerId: 1, 
            officerName: 'Officer Sarah', 
            actionType: 'ASSIGNED', 
            remarks: 'Assigned to Ward 05 field operations division.', 
            createdAt: demoInc.createdAt 
          },
          { 
            id: 2, 
            incidentId: demoInc.id, 
            officerId: 1, 
            officerName: 'Officer Sarah', 
            actionType: 'STARTED', 
            remarks: 'Excavation team on-site with suction vehicle.', 
            createdAt: demoInc.createdAt 
          },
          ...(demoInc.status === 'RESOLVED' || demoInc.status === 'VERIFIED' ? [{
            id: 3,
            incidentId: demoInc.id,
            officerId: 1,
            officerName: 'Officer Sarah', 
            actionType: 'RESOLVED', 
            remarks: 'Culvert cleared; road surface restored.', 
            createdAt: new Date().toISOString(),
          }] : [])
        ]);
        setError(null);
      } else {
        setError(err.message || `Failed to load intelligence details for case #${fetchId}`);
      }
    } finally {
      if (activeRequestIdRef.current === fetchId) {
        setLoading(false);
      }
    }
  }, [id]);

  useEffect(() => {
    activeRequestIdRef.current = id;
    void Promise.resolve().then(() => {
      if (activeRequestIdRef.current === id) {
        fetchIncidentData();
      }
    });
    return () => {
      activeRequestIdRef.current = null;
    };
  }, [fetchIncidentData, id]);

  // Get dynamic officer ID from active session
  const getCurrentOfficerId = () => {
    try {
      const raw = localStorage.getItem('nirvaha_user');
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed && parsed.id) return parsed.id;
      }
    } catch {
      // Fallback
    }
    return 1;
  };

  // Workflow Handlers
  const handleAssignOfficer = async () => {
    try {
      await workflowService.assignOfficer(id, getCurrentOfficerId(), 'Assigned to municipal field response unit');
      await fetchIncidentData();
    } catch (err) {
      setToast({ open: true, message: `Assignment error: ${err.message}`, type: 'error' });
    }
  };

  const handleStartWork = async () => {
    try {
      await workflowService.startWork(id, getCurrentOfficerId(), 'Field operations commenced at location');
      await fetchIncidentData();
    } catch (err) {
      setToast({ open: true, message: `Start work error: ${err.message}`, type: 'error' });
    }
  };

  const handleSubmitResolution = async (e) => {
    e.preventDefault();
    if (!resolutionFile) {
      setToast({ open: true, message: 'Please select a photographic evidence file showing completed work.', type: 'error' });
      return;
    }
    setSubmittingResolution(true);
    try {
      const officerId = getCurrentOfficerId();
      // 1. Upload photo evidence file
      const formData = new FormData();
      formData.append('file', resolutionFile);
      formData.append('description', resolutionRemarks || 'Post-remediation field verification photo');
      
      const uploadedEv = await evidenceService.uploadEvidence(id, formData);

      // 2. Submit resolution with evidence ID
      await resolutionService.submitResolution(id, {
        officerId,
        evidenceId: uploadedEv.id,
        remarks: resolutionRemarks,
      });

      // 3. Update workflow status to RESOLVED
      await workflowService.resolveIncident(id, officerId, resolutionRemarks || 'Remediation completed by field team');

      setShowResolutionModal(false);
      setResolutionRemarks('');
      setResolutionFile(null);
      await fetchIncidentData();
    } catch (err) {
      setToast({ open: true, message: `Resolution submission error: ${err.message}`, type: 'error' });
    } finally {
      setSubmittingResolution(false);
    }
  };

  const handleReopenCase = async (e) => {
    e.preventDefault();
    if (!reopenRemarks.trim()) {
      setToast({ open: true, message: 'Please enter justification remarks for reopening this case.', type: 'error' });
      return;
    }
    setSubmittingReopen(true);
    try {
      await workflowService.reopenIncident(id, getCurrentOfficerId(), reopenRemarks);
      setShowReopenModal(false);
      setReopenRemarks('');
      await fetchIncidentData();
    } catch (err) {
      setToast({ open: true, message: `Reopen case error: ${err.message}`, type: 'error' });
    } finally {
      setSubmittingReopen(false);
    }
  };

  const handleLogProgress = async (e) => {
    e.preventDefault();
    if (!progressRemarks.trim()) {
      setToast({ open: true, message: 'Please enter progress update notes.', type: 'error' });
      return;
    }
    setSubmittingProgress(true);
    try {
      await workflowService.recordProgress(id, getCurrentOfficerId(), progressRemarks);
      setShowProgressModal(false);
      setProgressRemarks('');
      await fetchIncidentData();
    } catch (err) {
      setToast({ open: true, message: `Progress log error: ${err.message}`, type: 'error' });
    } finally {
      setSubmittingProgress(false);
    }
  };

  if (loading) return <LoadingState message={`Retrieving municipal case file #${id}...`} />;
  if (error) {
    return (
      <ErrorState
        message={error}
        onRetry={() => {
          setLoading(true);
          setError(null);
          fetchIncidentData();
        }}
      />
    );
  }
  if (!incident) return null;

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
      {toast.open && (
        <FeedbackToast
          type={toast.type}
          message={toast.message}
          onClose={() => setToast({ open: false, message: '', type: 'info' })}
        />
      )}
      
      {/* Top Header Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#D9DEDB] pb-4">
        <div className="flex items-center space-x-3">
          <button
            onClick={() => navigate(-1)}
            className="p-1.5 bg-white hover:bg-[#F5F4EF] text-[#18201F] rounded border border-[#D9DEDB] transition shadow-xs"
            title="Return to incident list"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-mono text-xs text-[#2F6F68] font-bold">MUNICIPAL CASE FILE #{incident.id}</span>
              <Badge variant="cyan">{incident.classificationTag || incident.category}</Badge>
              <Badge variant={incident.severity === 'HIGH' || incident.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                {incident.severity} SEVERITY
              </Badge>
            </div>
            <h1 className="text-xl font-bold text-[#18201F] mt-0.5">{incident.title}</h1>
          </div>
        </div>

        {/* Status Badge & Actions */}
        <div className="flex items-center flex-wrap gap-2">
          <Badge 
            variant={
              incident.status === 'RESOLVED' || incident.status === 'VERIFIED' ? 'success' :
              incident.status === 'IN_PROGRESS' ? 'warning' : 'primary'
            }
            className="text-xs font-bold"
          >
            STATUS: {incident.status}
          </Badge>

          {incident.status === 'REPORTED' && (
            <button
              onClick={handleAssignOfficer}
              className="px-3 py-1.5 bg-[#163A36] hover:bg-[#2F6F68] text-white font-bold text-xs rounded transition flex items-center space-x-1.5 shadow-xs"
            >
              <UserCheck className="w-3.5 h-3.5" />
              <span>Assign Officer</span>
            </button>
          )}

          {incident.status === 'ASSIGNED' && (
            <button
              onClick={handleStartWork}
              className="px-3 py-1.5 bg-[#C58A32] hover:bg-[#a67225] text-white font-bold text-xs rounded transition flex items-center space-x-1.5 shadow-xs"
            >
              <Clock className="w-3.5 h-3.5" />
              <span>Start Work</span>
            </button>
          )}

          {(incident.status === 'IN_PROGRESS' || incident.status === 'ASSIGNED') && (
            <button
              onClick={() => setShowResolutionModal(true)}
              className="px-3 py-1.5 bg-[#3F7D5A] hover:bg-[#326447] text-white font-bold text-xs rounded transition flex items-center space-x-1.5 shadow-xs"
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Submit Resolution Evidence</span>
            </button>
          )}

          {(incident.status === 'RESOLVED' || resolution?.verificationStatus === 'FAILED' || resolution?.verificationStatus === 'MANUAL_REVIEW') && (
            <button
              onClick={() => setShowReopenModal(true)}
              className="px-3 py-1.5 bg-white hover:bg-[#B94A48]/5 text-[#B94A48] border border-[#B94A48]/30 font-bold text-xs rounded transition flex items-center space-x-1.5 shadow-xs"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Reopen Case</span>
            </button>
          )}
        </div>
      </div>

      {/* Navigation Tabs Bar */}
      <div className="flex items-center space-x-2 border-b border-[#D9DEDB]">
        <button
          onClick={() => setActiveTab('evidence')}
          className={`flex items-center space-x-2 px-4 py-2.5 font-bold text-xs border-b-2 transition ${
            activeTab === 'evidence'
              ? 'border-[#163A36] text-[#163A36] bg-[#163A36]/5'
              : 'border-transparent text-[#66716E] hover:text-[#18201F]'
          }`}
        >
          <Camera className="w-4 h-4 text-[#2F6F68]" />
          <span>Resolution Evidence & Verification</span>
          {resolution && (
            <Badge variant={resolution.verificationStatus === 'VERIFIED' ? 'success' : 'warning'} className="ml-1 text-[9px]">
              {resolution.verificationStatus}
            </Badge>
          )}
        </button>

        <button
          onClick={() => setActiveTab('intelligence')}
          className={`flex items-center space-x-2 px-4 py-2.5 font-bold text-xs border-b-2 transition ${
            activeTab === 'intelligence'
              ? 'border-[#163A36] text-[#163A36] bg-[#163A36]/5'
              : 'border-transparent text-[#66716E] hover:text-[#18201F]'
          }`}
        >
          <Layers className="w-4 h-4 text-[#C58A32]" />
          <span>Case Dossier & Intelligence Matrix</span>
          {duplicates.length > 0 && (
            <span className="px-1.5 py-0.2 bg-[#F5F4EF] rounded font-mono text-[9px] text-[#66716E] border border-[#D9DEDB]">
              +{duplicates.length} cluster
            </span>
          )}
        </button>
      </div>

      {/* Tab 1: Resolution Evidence Verification Center */}
      {activeTab === 'evidence' && (
        <ResolutionEvidenceVerification
          incident={incident}
          evidenceList={evidenceList}
          resolution={resolution}
          actions={actions}
          feedbackList={feedbackList}
          currentUser={currentUser}
          onRefresh={fetchIncidentData}
          onOpenResolutionModal={() => setShowResolutionModal(true)}
          onOpenReopenModal={() => setShowReopenModal(true)}
          onOpenProgressModal={() => setShowProgressModal(true)}
        />
      )}

      {/* Tab 2: Case Dossier & Intelligence Matrix */}
      {activeTab === 'intelligence' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
          
          {/* Left Column (7 cols): Citizen Voice & Evidence Vault */}
          <div className="lg:col-span-7 space-y-4">
            
            {/* Citizen Voice & Multilingual Inspection */}
            <Card className="space-y-3 border-[#D9DEDB]">
              <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2">
                <span className="text-xs font-bold text-[#18201F] uppercase tracking-wider flex items-center space-x-1.5">
                  <Globe className="w-4 h-4 text-[#2F6F68]" />
                  <span>Citizen Voice & Language Provenance</span>
                </span>
                <span className="px-2 py-0.5 rounded bg-[#F5F4EF] text-[#2F6F68] font-mono text-[10px] font-bold border border-[#D9DEDB]">
                  Language: {incident.originalLanguage || 'EN'}
                </span>
              </div>

              <div className="space-y-2 text-xs">
                <div>
                  <span className="text-[#66716E] font-mono text-[10px] uppercase block mb-1">Original Submitted Description:</span>
                  <p className="text-[#18201F] bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] italic leading-relaxed">
                    "{incident.originalDescription || incident.description}"
                  </p>
                </div>

                <div>
                  <span className="text-[#66716E] font-mono text-[10px] uppercase block mb-1">Normalized Civic Meaning:</span>
                  <p className="text-[#163A36] bg-[#163A36]/5 p-3 rounded border border-[#163A36]/20 font-mono leading-relaxed">
                    {incident.normalizedDescription || `${incident.category} ISSUE`}
                  </p>
                </div>

                <div className="flex justify-between items-center text-[10px] font-mono text-[#66716E] pt-1">
                  <span>Normalization: <strong className="text-[#3F7D5A]">{incident.normalizationStatus || 'COMPLETED'}</strong></span>
                  <span>Provider: <strong className="text-[#163A36]">{incident.languageProvider || 'LOCAL_RULES'}</strong></span>
                  <span>Confidence: <strong className="text-[#18201F]">{((incident.languageConfidence || 0.92) * 100).toFixed(0)}%</strong></span>
                </div>
              </div>
            </Card>

            {/* Attached Evidence Vault */}
            <Card className="space-y-3 border-[#D9DEDB]">
              <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2">
                <span className="text-xs font-bold text-[#18201F] uppercase tracking-wider flex items-center space-x-1.5">
                  <Camera className="w-4 h-4 text-[#66716E]" />
                  <span>Case Evidence Files ({evidenceList.length})</span>
                </span>
                <span className="text-[10px] font-mono text-[#66716E]">Ingest Fingerprinted</span>
              </div>

              {evidenceList.length === 0 ? (
                <div className="text-center py-6 text-xs text-[#66716E]">No photographic evidence attached to this case.</div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {evidenceList.map((ev) => (
                    <div key={ev.id} className="bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] space-y-2 text-xs">
                      <div className="h-36 bg-white rounded overflow-hidden flex items-center justify-center border border-[#D9DEDB]">
                        {ev.fileUrl ? (
                          <img src={ev.fileUrl} alt={ev.description} className="w-full h-full object-cover" />
                        ) : (
                          <div className="text-[#66716E] text-center font-mono text-[10px]">
                            <Camera className="w-6 h-6 mx-auto mb-1 text-[#66716E]" />
                            <span>Photo Evidence File #{ev.id}</span>
                          </div>
                        )}
                      </div>
                      <div className="space-y-1 font-mono text-[10px] text-[#66716E]">
                        <div className="text-[#18201F] font-semibold">{ev.description || 'Citizen evidence photo'}</div>
                        <div>Type: <span className="text-[#18201F]">{ev.evidenceType}</span></div>
                        <div>Captured: <span className="text-[#18201F]">{ev.capturedAt ? new Date(ev.capturedAt).toLocaleString() : 'N/A'}</span></div>
                        {ev.contentHash && (
                          <div className="truncate">Hash: <span className="text-[#18201F] select-all">{ev.contentHash.substring(0, 16)}...</span></div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>

          </div>

          {/* Right Column (5 cols): Priority, Duplicates, Integrity, Provenance */}
          <div className="lg:col-span-5 space-y-4">
            
            {/* Priority & SLA Context Panel */}
            <Card className="space-y-3 border-[#D9DEDB]">
              <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2">
                <span className="text-xs font-bold text-[#18201F] uppercase tracking-wider flex items-center space-x-1.5">
                  <ShieldAlert className="w-4 h-4 text-[#C58A32]" />
                  <span>Priority Intelligence Breakdown</span>
                </span>
                <span className="font-mono font-bold text-[#C58A32] text-sm">
                  {priority ? `${priority.priorityScore.toFixed(1)} / 100` : 'N/A'}
                </span>
              </div>

              {priority ? (
                <div className="space-y-2.5 text-xs">
                  <div className="bg-[#F5F4EF] p-2.5 rounded border border-[#D9DEDB] space-y-1.5 font-mono text-[11px]">
                    <div className="flex justify-between">
                      <span className="text-[#66716E]">Base Severity Factor:</span>
                      <span className="text-[#18201F] font-bold">{priority.baseSeverityScore}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[#66716E]">Spatial Corroboration:</span>
                      <span className="text-[#18201F] font-bold">+{priority.corroborationScore} ({priority.linkedDuplicatesCount} reports)</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[#66716E]">Weather Context Multiplier:</span>
                      <span className="text-[#2F6F68] font-bold">+{priority.weatherMultiplier}</span>
                    </div>
                    <div className="flex justify-between border-t border-[#D9DEDB] pt-1">
                      <span className="text-[#66716E]">SLA Target ({incident.targetDays || 15}d):</span>
                      <span className="font-bold">
                        {incident.slaStatus === 'OVERDUE' ? (
                          <span className="text-[#B94A48]">Target Exceeded ({Math.abs(incident.remainingDays || 0)}d overdue)</span>
                        ) : incident.slaStatus === 'APPROACHING_DEADLINE' ? (
                          <span className="text-[#C58A32]">Approaching ({incident.remainingDays || 0}d left)</span>
                        ) : incident.slaStatus === 'WITHIN_STANDARD' ? (
                          <span className="text-[#2F6F68]">Within Standard ({incident.remainingDays || 0}d left)</span>
                        ) : incident.slaStatus === 'COMPLETED' ? (
                          <span className="text-[#3F7D5A]">Resolved in SLA</span>
                        ) : (
                          <span className="text-[#66716E]">Proposed Target ({incident.targetDays || 5}d)</span>
                        )}
                      </span>
                    </div>
                  </div>

                  <div className="bg-[#F5F4EF] p-2.5 rounded border border-[#D9DEDB] space-y-1">
                    <span className="text-[10px] font-mono text-[#2F6F68] uppercase font-bold block">IMD Weather Context:</span>
                    <p className="text-[11px] text-[#18201F] leading-relaxed">{incident.weatherContext || 'Weather telemetry normal'}</p>
                  </div>
                </div>
              ) : (
                <div className="text-center py-4 text-xs text-[#66716E]">Priority evaluation initializing...</div>
              )}
            </Card>

            {/* Spatial Corroboration & Linked Duplicates */}
            <Card className="space-y-3 border-[#D9DEDB]">
              <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2">
                <span className="text-xs font-bold text-[#18201F] uppercase tracking-wider flex items-center space-x-1.5">
                  <Layers className="w-4 h-4 text-[#C87941]" />
                  <span>Spatial Corroboration ({duplicates.length})</span>
                </span>
                <span className="text-[10px] font-mono text-[#66716E]">500m Radius</span>
              </div>

              {duplicates.length === 0 ? (
                <div className="text-center py-4 text-xs text-[#66716E]">No linked duplicate reports detected within 500m.</div>
              ) : (
                <div className="space-y-2 text-xs">
                  {duplicates.map((dup) => (
                    <div key={dup.id} className="bg-[#F5F4EF] p-2.5 rounded border border-[#D9DEDB] space-y-1">
                      <div className="flex justify-between items-center">
                        <span className="font-mono text-[10px] text-[#2F6F68] font-bold">CASE #{dup.id}</span>
                        <span className="text-[10px] font-mono text-[#66716E]">{(dup.similarityScore * 100).toFixed(0)}% Match</span>
                      </div>
                      <p className="text-[#18201F] font-semibold text-[11px]">{dup.title}</p>
                    </div>
                  ))}
                </div>
              )}
            </Card>

            {/* Integrity Radar Notice */}
            {anomalies.length > 0 && (
              <Card className="space-y-3 border-[#B94A48]/30 bg-[#B94A48]/5">
                <div className="flex items-center justify-between border-b border-[#B94A48]/20 pb-2">
                  <span className="text-xs font-bold text-[#B94A48] uppercase tracking-wider flex items-center space-x-1.5">
                    <AlertTriangle className="w-4 h-4 text-[#B94A48]" />
                    <span>Integrity Radar Notice</span>
                  </span>
                  <Badge variant="danger">REVIEW RECOMMENDED</Badge>
                </div>

                <div className="space-y-2 text-xs">
                  {anomalies.map((ano) => (
                    <div key={ano.id} className="bg-white p-2.5 rounded border border-[#D9DEDB] space-y-1">
                      <span className="font-mono text-[10px] text-[#B94A48] font-bold uppercase">{ano.flagType}</span>
                      <p className="text-[#18201F] text-[11px]">{ano.reason}</p>
                    </div>
                  ))}
                </div>
              </Card>
            )}

            {/* Data Provenance & Location Metadata */}
            <Card className="space-y-3 border-[#D9DEDB]">
              <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2">
                <span className="text-xs font-bold text-[#18201F] uppercase tracking-wider flex items-center space-x-1.5">
                  <MapPin className="w-4 h-4 text-[#2F6F68]" />
                  <span>Geospatial & Provenance Ledger</span>
                </span>
              </div>

              <div className="space-y-1.5 text-xs font-mono text-[11px] text-[#18201F]">
                <div>Address: <span className="text-[#18201F] font-sans">{incident.address}</span></div>
                <div>Ward: <span className="text-[#2F6F68]">{incident.assignedAreaName || 'UNASSIGNED'}</span></div>
                <div>Coordinates: <span className="text-[#66716E]">{incident.latitude}, {incident.longitude}</span></div>
                <div>Assignment Method: <span className="text-[#66716E]">{incident.assignmentMethod || 'POINT_IN_POLYGON'}</span></div>
                <div>Data Source: <span className="text-[#66716E]">{incident.dataSourceName || 'Nirvaha Evidence Network'}</span></div>
                {incident.nearestRoadName ? (
                  <div className="p-2 bg-emerald-50/70 rounded border border-emerald-200/80 space-y-1 text-[11px] my-1">
                    <div className="flex items-center justify-between font-bold text-emerald-900">
                      <span>Nearest Road Reference:</span>
                      <span className="text-[10px] font-mono bg-emerald-100 text-emerald-800 px-1.5 py-0.2 rounded">
                        {incident.nearestRoadConfidence || 'MEDIUM'} CONFIDENCE
                      </span>
                    </div>
                    <div className="text-emerald-950 font-sans font-semibold">
                      {incident.nearestRoadName}
                    </div>
                    <div className="text-[#64748B] text-[10px] flex items-center justify-between">
                      <span>Proximity: {incident.nearestRoadDistanceMeters != null ? `${Math.round(incident.nearestRoadDistanceMeters)}m` : 'N/A'}</span>
                      <span>Source: OpenStreetMap (ODbL 1.0)</span>
                    </div>
                    {incident.roadContextExplanation && (
                      <p className="text-[10px] text-[#475569] italic pt-0.5">
                        {incident.roadContextExplanation}
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="text-[10px] text-[#64748B] italic">
                    Road Reference: No arterial road match within 300m search radius.
                  </div>
                )}
                <div className="flex items-center justify-between flex-wrap gap-1">
                  <div>
                    Service Standard: <span className="text-[#18201F]">{incident.serviceRuleDescription || 'Standard Grievance Resolution'}</span>
                  </div>
                  {incident.isOfficialSla ? (
                    <span className="text-[9px] bg-emerald-100 text-emerald-800 font-bold px-1.5 py-0.5 rounded border border-emerald-300">
                      OFFICIAL SLA ({incident.targetDays || 15}d)
                    </span>
                  ) : (
                    <span className="text-[9px] bg-amber-100 text-amber-800 font-bold px-1.5 py-0.5 rounded border border-amber-300">
                      PROPOSED TARGET ({incident.targetDays || 5}d)
                    </span>
                  )}
                </div>
                {incident.dueAt && (
                  <div className="flex justify-between items-center text-[10px] text-[#66716E] pt-0.5">
                    <span>Deadline: <span className="text-[#18201F] font-sans">{new Date(incident.dueAt).toLocaleDateString()}</span></span>
                    {incident.escalationLevel > 0 && (
                      <span className="text-amber-700 font-sans font-bold">
                        • Escalation Level {incident.escalationLevel} Notified
                      </span>
                    )}
                  </div>
                )}
              </div>
            </Card>

          </div>

        </div>
      )}

      {/* Modal 1: Submit Resolution Evidence */}
      {showResolutionModal && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <form onSubmit={handleSubmitResolution} className="bg-white border border-[#D9DEDB] rounded-lg max-w-lg w-full p-5 space-y-4 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <CheckCircle2 className="w-5 h-5 text-[#3F7D5A]" />
                <h3 className="text-base font-bold text-[#18201F]">Submit Resolution Evidence</h3>
              </div>
              <button 
                type="button" 
                onClick={() => setShowResolutionModal(false)} 
                className="text-[#66716E] hover:text-[#18201F] font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-[#18201F] mb-1">
                  Field Officer Work Remarks:
                </label>
                <textarea
                  rows={3}
                  required
                  value={resolutionRemarks}
                  onChange={(e) => setResolutionRemarks(e.target.value)}
                  placeholder="Describe repair work completed (e.g. Cleared drainage blockage, aligned culvert, tested flow)..."
                  className="w-full bg-[#F5F4EF] border border-[#D9DEDB] rounded p-2.5 text-xs text-[#18201F] focus:outline-none focus:border-[#163A36]"
                />
              </div>

              <div>
                <label className="block font-bold text-[#18201F] mb-1">
                  Post-Action Photographic Proof:
                </label>
                <input
                  type="file"
                  accept="image/*"
                  required
                  onChange={(e) => setResolutionFile(e.target.files[0])}
                  className="w-full bg-[#F5F4EF] border border-[#D9DEDB] rounded p-2 text-xs text-[#18201F]"
                />
                <span className="text-[10px] text-[#66716E] mt-1 block">
                  File will be SHA-256 fingerprinted at upload and compared against the incident location.
                </span>
              </div>
            </div>

            <div className="pt-2 border-t border-[#D9DEDB] flex justify-end space-x-2">
              <button
                type="button"
                onClick={() => setShowResolutionModal(false)}
                className="px-3 py-1.5 bg-[#F5F4EF] hover:bg-[#e0ded8] text-[#18201F] text-xs font-bold rounded"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submittingResolution}
                className="px-3 py-1.5 bg-[#3F7D5A] hover:bg-[#326447] text-white text-xs font-bold rounded transition disabled:opacity-50"
              >
                {submittingResolution ? 'Fingerprinting & Submitting...' : 'Submit Resolution'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Modal 2: Reopen Case Modal */}
      {showReopenModal && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <form onSubmit={handleReopenCase} className="bg-white border border-[#D9DEDB] rounded-lg max-w-lg w-full p-5 space-y-4 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <RotateCcw className="w-5 h-5 text-[#B94A48]" />
                <h3 className="text-base font-bold text-[#18201F]">Reopen Municipal Case File</h3>
              </div>
              <button 
                type="button" 
                onClick={() => setShowReopenModal(false)} 
                className="text-[#66716E] hover:text-[#18201F] font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-[#66716E] leading-relaxed">
              Reopening resets the incident status to <strong>REPORTED</strong> and places it back in the officer action queue. Use this when photo verification fails, workmanship is substandard, or citizen corroboration indicates the issue persists.
            </p>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-[#18201F] mb-1">
                  Justification Remarks for Reopening:
                </label>
                <textarea
                  rows={3}
                  required
                  value={reopenRemarks}
                  onChange={(e) => setReopenRemarks(e.target.value)}
                  placeholder="State reason for reopening (e.g. Photo evidence failed proximity check; debris remains in drainage mouth)..."
                  className="w-full bg-[#F5F4EF] border border-[#D9DEDB] rounded p-2.5 text-xs text-[#18201F] focus:outline-none focus:border-[#B94A48]"
                />
              </div>
            </div>

            <div className="pt-2 border-t border-[#D9DEDB] flex justify-end space-x-2">
              <button
                type="button"
                onClick={() => setShowReopenModal(false)}
                className="px-3 py-1.5 bg-[#F5F4EF] hover:bg-[#e0ded8] text-[#18201F] text-xs font-bold rounded"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submittingReopen}
                className="px-3 py-1.5 bg-[#B94A48] hover:bg-[#9E3937] text-white text-xs font-bold rounded transition disabled:opacity-50"
              >
                {submittingReopen ? 'Processing...' : 'Confirm Reopen'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Modal 3: Log Progress Modal */}
      {showProgressModal && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <form onSubmit={handleLogProgress} className="bg-white border border-[#D9DEDB] rounded-lg max-w-lg w-full p-5 space-y-4 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <PlusCircle className="w-5 h-5 text-[#2F6F68]" />
                <h3 className="text-base font-bold text-[#18201F]">Log Field Progress Note</h3>
              </div>
              <button 
                type="button" 
                onClick={() => setShowProgressModal(false)} 
                className="text-[#66716E] hover:text-[#18201F] font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-[#18201F] mb-1">
                  Field Update Note:
                </label>
                <textarea
                  rows={3}
                  required
                  value={progressRemarks}
                  onChange={(e) => setProgressRemarks(e.target.value)}
                  placeholder="Record interim status (e.g. Suction unit dispatched; 40% cleared; waiting for road closure)..."
                  className="w-full bg-[#F5F4EF] border border-[#D9DEDB] rounded p-2.5 text-xs text-[#18201F] focus:outline-none focus:border-[#2F6F68]"
                />
              </div>
            </div>

            <div className="pt-2 border-t border-[#D9DEDB] flex justify-end space-x-2">
              <button
                type="button"
                onClick={() => setShowProgressModal(false)}
                className="px-3 py-1.5 bg-[#F5F4EF] hover:bg-[#e0ded8] text-[#18201F] text-xs font-bold rounded"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submittingProgress}
                className="px-3 py-1.5 bg-[#2F6F68] hover:bg-[#163A36] text-white text-xs font-bold rounded transition disabled:opacity-50"
              >
                {submittingProgress ? 'Logging...' : 'Save Progress Note'}
              </button>
            </div>
          </form>
        </div>
      )}

    </div>
  );
};
