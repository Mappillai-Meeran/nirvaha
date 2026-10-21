import React, { useState } from 'react';
import { Card, Badge } from '../ui';
import { feedbackService } from '../../services';
import { 
  CheckCircle2, 
  AlertTriangle, 
  XCircle, 
  Camera, 
  Clock, 
  ShieldCheck, 
  FileCheck, 
  MapPin, 
  HardDrive, 
  Copy, 
  Check, 
  RotateCcw, 
  Eye, 
  PlusCircle, 
  HelpCircle,
  MessageSquare,
  ThumbsUp,
  ThumbsDown,
  UserCheck,
  Loader2,
  Info
} from 'lucide-react';

/**
 * ResolutionEvidenceVerification
 * 
 * Provides an authoritative, transparent evidence verification review interface.
 * Implements side-by-side comparison between original reporting evidence and submitted
 * post-repair resolution evidence, presents backend similarity telemetry, displays
 * SHA-256 cryptographic fingerprints, and renders the chronological audit trail.
 */
export const ResolutionEvidenceVerification = ({
  incident,
  evidenceList = [],
  resolution = null,
  actions = [],
  feedbackList = [],
  currentUser = null,
  onRefresh,
  onOpenResolutionModal,
  onOpenReopenModal,
  onOpenProgressModal,
}) => {
  const [viewMode, setViewMode] = useState('side-by-side'); // 'side-by-side' | 'focused-before' | 'focused-after'
  const [copiedHash, setCopiedHash] = useState(null);
  const [activeLightboxImg, setActiveLightboxImg] = useState(null);

  // Resident Feedback & Dispute States
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [showDisputeModal, setShowDisputeModal] = useState(false);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [selectedFeedbackForReview, setSelectedFeedbackForReview] = useState(null);

  const [confirmNotes, setConfirmNotes] = useState('');
  const [confirmError, setConfirmError] = useState(null);

  const [disputeExplanation, setDisputeExplanation] = useState('');
  const [disputeError, setDisputeError] = useState(null);

  const [reviewStatus, setReviewStatus] = useState('ACCEPTED_FOR_REWORK');
  const [reviewNotes, setReviewNotes] = useState('');
  const [reviewError, setReviewError] = useState(null);

  const [submittingFeedback, setSubmittingFeedback] = useState(false);
  const [feedbackSuccessToast, setFeedbackSuccessToast] = useState(null);

  const handleConfirmResolution = async () => {
    setSubmittingFeedback(true);
    setConfirmError(null);
    try {
      await feedbackService.submitFeedback(incident.id, {
        feedbackType: 'CONFIRMED',
        explanation: confirmNotes.trim() || 'Resident confirmed resolution of reported civic issue.'
      });
      setShowConfirmModal(false);
      setConfirmNotes('');
      setFeedbackSuccessToast('Resolution confirmed successfully. The civic resolution has been validated.');
      if (onRefresh) await onRefresh();
    } catch (err) {
      setConfirmError(err.message || 'Failed to submit resolution confirmation.');
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const handleDisputeResolution = async (e) => {
    e.preventDefault();
    const trimmed = disputeExplanation.trim();
    if (!trimmed) {
      setDisputeError('A detailed explanation is required describing why the issue remains unresolved.');
      return;
    }
    if (trimmed.length > 2000) {
      setDisputeError('Explanation cannot exceed 2000 characters.');
      return;
    }
    setSubmittingFeedback(true);
    setDisputeError(null);
    try {
      await feedbackService.submitFeedback(incident.id, {
        feedbackType: 'DISPUTED',
        explanation: trimmed
      });
      setShowDisputeModal(false);
      setDisputeExplanation('');
      setFeedbackSuccessToast('Dispute submitted successfully. The incident has been routed to supervisory review and scheduled for field re-inspection.');
      if (onRefresh) await onRefresh();
    } catch (err) {
      setDisputeError(err.message || 'Failed to submit resolution dispute.');
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const handleReviewDispute = async (e) => {
    e.preventDefault();
    if (!selectedFeedbackForReview) return;
    setSubmittingFeedback(true);
    setReviewError(null);
    try {
      await feedbackService.reviewDispute(incident.id, selectedFeedbackForReview.id, {
        status: reviewStatus,
        reviewNotes: reviewNotes.trim() || undefined
      });
      setShowReviewModal(false);
      setReviewNotes('');
      setSelectedFeedbackForReview(null);
      setFeedbackSuccessToast('Dispute review decision successfully recorded in municipal ledger.');
      if (onRefresh) await onRefresh();
    } catch (err) {
      setReviewError(err.message || 'Failed to record dispute review.');
    } finally {
      setSubmittingFeedback(false);
    }
  };

  // Identify original evidence (first citizen evidence) and resolution evidence
  const originalEvidence = evidenceList.length > 0 ? evidenceList[0] : null;
  const resolutionEvidence = resolution && resolution.resolutionEvidenceId
    ? evidenceList.find((e) => e.id === resolution.resolutionEvidenceId) || {
        id: resolution.resolutionEvidenceId,
        fileUrl: resolution.resolutionEvidenceUrl,
        description: 'Post-repair resolution evidence',
      }
    : null;

  const copyToClipboard = (text, id) => {
    navigator.clipboard.writeText(text);
    setCopiedHash(id);
    setTimeout(() => setCopiedHash(null), 2000);
  };

  const formatFileSize = (bytes) => {
    if (!bytes || isNaN(bytes)) return 'N/A';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  // Determine verification state badge and messaging
  const getVerificationStatusMeta = () => {
    if (!resolution) {
      return {
        variant: 'default',
        label: 'PENDING RESOLUTION',
        icon: Clock,
        headline: 'Resolution Evidence Awaited',
        description: 'Field operations are in progress. Post-work photo evidence has not yet been submitted for algorithmic verification.',
        bgClass: 'bg-[#F5F4EF] border-[#D9DEDB]',
        textClass: 'text-[#66716E]',
      };
    }

    switch (resolution.verificationStatus) {
      case 'VERIFIED':
        return {
          variant: 'success',
          label: 'VERIFIED',
          icon: CheckCircle2,
          headline: 'Evidence Supports Resolution',
          description: 'Algorithmic proximity is within the 500-meter municipal tolerance and photo telemetry correlates with the original incident location.',
          bgClass: 'bg-[#3F7D5A]/10 border-[#3F7D5A]/30',
          textClass: 'text-[#3F7D5A]',
        };
      case 'MANUAL_REVIEW':
        return {
          variant: 'warning',
          label: 'MANUAL REVIEW RECOMMENDED',
          icon: AlertTriangle,
          headline: 'Supervisory Review Required',
          description: 'Resolution evidence exhibits partial proximity or payload divergence (Score: 40%–74%). Human supervisor visual inspection required before sign-off.',
          bgClass: 'bg-[#C58A32]/10 border-[#C58A32]/30',
          textClass: 'text-[#C58A32]',
        };
      case 'FAILED':
        return {
          variant: 'danger',
          label: 'VERIFICATION FAILED',
          icon: XCircle,
          headline: 'Insufficient Evidence Correlation',
          description: 'Resolution evidence diverges substantially (>500m distance or incompatible payload). Case reopening is recommended for field re-inspection.',
          bgClass: 'bg-[#B94A48]/10 border-[#B94A48]/30',
          textClass: 'text-[#B94A48]',
        };
      default:
        return {
          variant: 'default',
          label: resolution.verificationStatus || 'UNKNOWN',
          icon: HelpCircle,
          headline: 'Verification Status Recorded',
          description: resolution.remarks || 'Status evaluated by resolution service.',
          bgClass: 'bg-[#F5F4EF] border-[#D9DEDB]',
          textClass: 'text-[#18201F]',
        };
    }
  };

  const statusMeta = getVerificationStatusMeta();
  const StatusIcon = statusMeta.icon;

  const similarityScorePercent = resolution?.resolutionSimilarityScore != null
    ? Math.round(resolution.resolutionSimilarityScore * 100)
    : null;

  return (
    <div className="space-y-6">
      
      {/* 1. Primary Verification Status & Algorithmic Summary Banner */}
      <Card className={`border ${statusMeta.bgClass} p-5 space-y-4 shadow-sm`}>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 border-b border-[#D9DEDB]/70 pb-3">
          <div className="flex items-center space-x-3">
            <div className={`p-2 rounded-full bg-white shadow-xs border border-[#D9DEDB]`}>
              <StatusIcon className={`w-5 h-5 ${statusMeta.textClass}`} />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-mono text-xs font-bold uppercase tracking-wider text-[#66716E]">
                  Resolution Verification Status
                </span>
                <Badge variant={statusMeta.variant}>{statusMeta.label}</Badge>
              </div>
              <h2 className="text-base font-bold text-[#18201F] mt-0.5">{statusMeta.headline}</h2>
            </div>
          </div>

          {/* Action buttons (Submit, Reopen, Add Progress) */}
          <div className="flex items-center space-x-2">
            {!resolution && (incident.status === 'IN_PROGRESS' || incident.status === 'ASSIGNED') && (
              <button
                onClick={onOpenResolutionModal}
                className="px-3 py-1.5 bg-[#3F7D5A] hover:bg-[#326447] text-white font-bold text-xs rounded transition flex items-center space-x-1.5 shadow-sm"
              >
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>Submit Resolution Evidence</span>
              </button>
            )}

            {incident.status === 'IN_PROGRESS' && (
              <button
                onClick={onOpenProgressModal}
                className="px-3 py-1.5 bg-white hover:bg-[#F5F4EF] text-[#18201F] border border-[#D9DEDB] font-bold text-xs rounded transition flex items-center space-x-1.5 shadow-xs"
              >
                <PlusCircle className="w-3.5 h-3.5 text-[#2F6F68]" />
                <span>Log Progress Note</span>
              </button>
            )}

            {(incident.status === 'RESOLVED' || resolution?.verificationStatus === 'FAILED' || resolution?.verificationStatus === 'MANUAL_REVIEW') && (
              <button
                onClick={onOpenReopenModal}
                className="px-3 py-1.5 bg-white hover:bg-[#B94A48]/5 text-[#B94A48] border border-[#B94A48]/30 font-bold text-xs rounded transition flex items-center space-x-1.5 shadow-xs"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Reopen Case for Field Re-inspection</span>
              </button>
            )}
          </div>
        </div>

        <p className="text-xs text-[#18201F]/80 leading-relaxed max-w-4xl">
          {statusMeta.description}
        </p>

        {/* Telemetry Breakdown (if resolution exists) */}
        {resolution && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2 text-xs">
            <div className="bg-white p-3 rounded border border-[#D9DEDB] space-y-1">
              <span className="text-[#66716E] font-mono text-[10px] uppercase block font-semibold">
                Combined Similarity Score
              </span>
              <div className="flex items-baseline space-x-2">
                <span className="font-mono text-xl font-extrabold text-[#18201F]">
                  {similarityScorePercent}%
                </span>
                <span className="font-mono text-[10px] text-[#66716E]">
                  ({resolution.resolutionSimilarityScore?.toFixed(2)} / 1.00)
                </span>
              </div>
              <div className="w-full bg-[#D9DEDB] h-1.5 rounded-full overflow-hidden mt-1">
                <div 
                  className={`h-full ${
                    similarityScorePercent >= 75 ? 'bg-[#3F7D5A]' :
                    similarityScorePercent >= 40 ? 'bg-[#C58A32]' : 'bg-[#B94A48]'
                  }`}
                  style={{ width: `${Math.min(100, Math.max(0, similarityScorePercent))}%` }}
                />
              </div>
            </div>

            <div className="bg-white p-3 rounded border border-[#D9DEDB] space-y-1">
              <span className="text-[#66716E] font-mono text-[10px] uppercase block font-semibold">
                Spatial Proximity Factor (60%)
              </span>
              <div className="font-mono text-xs text-[#18201F] font-bold">
                Haversine Proximity Tolerance: 500m
              </div>
              <p className="text-[10px] text-[#66716E] leading-snug">
                Scores 1.0 at 0m divergence, scaling down linearly to 0.0 at 500m distance.
              </p>
            </div>

            <div className="bg-white p-3 rounded border border-[#D9DEDB] space-y-1">
              <span className="text-[#66716E] font-mono text-[10px] uppercase block font-semibold">
                Payload Telemetry Factor (40%)
              </span>
              <div className="font-mono text-xs text-[#18201F] font-bold">
                File Size Difference Ratio
              </div>
              <p className="text-[10px] text-[#66716E] leading-snug">
                Compares binary byte volume between reporting and remediation uploads.
              </p>
            </div>
          </div>
        )}
      </Card>

      {/* 2. Before / After Visual Evidence Comparator */}
      <Card className="border-[#D9DEDB] space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#D9DEDB] pb-3">
          <div className="flex items-center space-x-2">
            <Camera className="w-4 h-4 text-[#2F6F68]" />
            <h3 className="text-sm font-bold text-[#18201F] uppercase tracking-wider">
              Visual Evidence Comparator: Before & After
            </h3>
          </div>

          {/* View Mode Switcher */}
          <div className="flex items-center space-x-1 bg-[#F5F4EF] p-1 rounded border border-[#D9DEDB] text-xs">
            <button
              onClick={() => setViewMode('side-by-side')}
              className={`px-2.5 py-1 rounded font-medium transition ${
                viewMode === 'side-by-side'
                  ? 'bg-white text-[#18201F] shadow-xs font-bold'
                  : 'text-[#66716E] hover:text-[#18201F]'
              }`}
            >
              Side-by-Side
            </button>
            <button
              onClick={() => setViewMode('focused-before')}
              className={`px-2.5 py-1 rounded font-medium transition ${
                viewMode === 'focused-before'
                  ? 'bg-white text-[#18201F] shadow-xs font-bold'
                  : 'text-[#66716E] hover:text-[#18201F]'
              }`}
            >
              Initial Only
            </button>
            <button
              onClick={() => setViewMode('focused-after')}
              className={`px-2.5 py-1 rounded font-medium transition ${
                viewMode === 'focused-after'
                  ? 'bg-white text-[#18201F] shadow-xs font-bold'
                  : 'text-[#66716E] hover:text-[#18201F]'
              }`}
            >
              Resolution Only
            </button>
          </div>
        </div>

        {/* Image Comparison Grid */}
        <div className={`grid gap-4 ${
          viewMode === 'side-by-side' ? 'grid-cols-1 md:grid-cols-2' : 'grid-cols-1'
        }`}>
          
          {/* Card: BEFORE / Initial Evidence */}
          {(viewMode === 'side-by-side' || viewMode === 'focused-before') && (
            <div className="bg-[#F5F4EF] rounded-lg border border-[#D9DEDB] overflow-hidden flex flex-col">
              <div className="px-4 py-2.5 bg-[#EFECE6] border-b border-[#D9DEDB] flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="w-2 h-2 rounded-full bg-[#B94A48]" />
                  <span className="font-mono text-xs font-bold uppercase text-[#18201F]">
                    BEFORE — Initial Incident Evidence
                  </span>
                </div>
                <Badge variant="cyan">CITIZEN INGEST</Badge>
              </div>

              {/* Photo Area */}
              <div className="relative bg-black/5 aspect-4/3 flex items-center justify-center overflow-hidden border-b border-[#D9DEDB]">
                {originalEvidence && originalEvidence.fileUrl ? (
                  <>
                    <img 
                      src={originalEvidence.fileUrl} 
                      alt="Initial incident" 
                      className="w-full h-full object-cover cursor-pointer hover:opacity-95 transition"
                      onClick={() => setActiveLightboxImg({ url: originalEvidence.fileUrl, title: 'Original Citizen Evidence' })}
                    />
                    <button
                      onClick={() => setActiveLightboxImg({ url: originalEvidence.fileUrl, title: 'Original Citizen Evidence' })}
                      className="absolute bottom-2 right-2 p-1.5 bg-black/60 hover:bg-black/80 text-white rounded text-[11px] flex items-center space-x-1 backdrop-blur-xs"
                      title="Inspect full resolution"
                    >
                      <Eye className="w-3.5 h-3.5" />
                      <span>Inspect</span>
                    </button>
                  </>
                ) : (
                  <div className="text-center p-6 text-[#66716E] space-y-2">
                    <Camera className="w-8 h-8 mx-auto text-[#66716E]/40" />
                    <p className="text-xs font-medium">No initial photo evidence uploaded</p>
                    <p className="text-[10px] text-[#66716E]">Incident created via verbal / text report.</p>
                  </div>
                )}
              </div>

              {/* Evidence Metadata Drawer */}
              <div className="p-4 space-y-2.5 text-xs font-mono bg-white flex-1 flex flex-col justify-between">
                <div className="space-y-1.5">
                  <div className="text-xs font-sans font-bold text-[#18201F]">
                    {originalEvidence?.description || incident.title}
                  </div>
                  <div className="flex items-center justify-between text-[#66716E] text-[11px] pt-1 border-t border-[#D9DEDB]/60">
                    <span className="flex items-center space-x-1">
                      <Clock className="w-3 h-3 text-[#66716E]" />
                      <span>Captured:</span>
                    </span>
                    <span className="text-[#18201F] font-semibold">
                      {originalEvidence?.capturedAt 
                        ? new Date(originalEvidence.capturedAt).toLocaleString() 
                        : (incident.createdAt ? new Date(incident.createdAt).toLocaleString() : 'N/A')}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-[#66716E] text-[11px]">
                    <span className="flex items-center space-x-1">
                      <MapPin className="w-3 h-3 text-[#2F6F68]" />
                      <span>Coordinates:</span>
                    </span>
                    <span className="text-[#18201F]">
                      {originalEvidence?.latitude ?? incident.latitude}, {originalEvidence?.longitude ?? incident.longitude}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-[#66716E] text-[11px]">
                    <span className="flex items-center space-x-1">
                      <HardDrive className="w-3 h-3 text-[#66716E]" />
                      <span>Payload Size:</span>
                    </span>
                    <span className="text-[#18201F]">
                      {formatFileSize(originalEvidence?.fileSize)}
                    </span>
                  </div>
                </div>

                {/* SHA-256 Fingerprint */}
                <div className="bg-[#F5F4EF] p-2.5 rounded border border-[#D9DEDB] space-y-1 text-[10px]">
                  <div className="flex items-center justify-between text-[#66716E] font-bold">
                    <span className="flex items-center space-x-1">
                      <FileCheck className="w-3 h-3 text-[#2F6F68]" />
                      <span>SHA-256 File Checksum</span>
                    </span>
                    {originalEvidence?.contentHash && (
                      <button
                        onClick={() => copyToClipboard(originalEvidence.contentHash, 'orig')}
                        className="text-[#2F6F68] hover:text-[#163A36] flex items-center space-x-0.5"
                        title="Copy cryptographic hash"
                      >
                        {copiedHash === 'orig' ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                        <span>{copiedHash === 'orig' ? 'Copied' : 'Copy'}</span>
                      </button>
                    )}
                  </div>
                  <div className="break-all font-mono text-[#18201F] select-all bg-white p-1 rounded border border-[#D9DEDB]/50">
                    {originalEvidence?.contentHash || 'Calculated at ingestion for duplicate prevention'}
                  </div>
                  <span className="text-[9px] text-[#66716E] block italic">
                    Prevents cross-case duplicate image reuse.
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Card: AFTER / Submitted Resolution Evidence */}
          {(viewMode === 'side-by-side' || viewMode === 'focused-after') && (
            <div className="bg-[#F5F4EF] rounded-lg border border-[#D9DEDB] overflow-hidden flex flex-col">
              <div className="px-4 py-2.5 bg-[#EFECE6] border-b border-[#D9DEDB] flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="w-2 h-2 rounded-full bg-[#3F7D5A]" />
                  <span className="font-mono text-xs font-bold uppercase text-[#18201F]">
                    AFTER — Post-Remediation Evidence
                  </span>
                </div>
                <Badge variant={resolution ? 'success' : 'default'}>
                  {resolution ? 'OFFICER SUBMISSION' : 'AWAITED'}
                </Badge>
              </div>

              {/* Photo Area */}
              <div className="relative bg-black/5 aspect-4/3 flex items-center justify-center overflow-hidden border-b border-[#D9DEDB]">
                {resolutionEvidence && resolutionEvidence.fileUrl ? (
                  <>
                    <img 
                      src={resolutionEvidence.fileUrl} 
                      alt="Resolution evidence" 
                      className="w-full h-full object-cover cursor-pointer hover:opacity-95 transition"
                      onClick={() => setActiveLightboxImg({ url: resolutionEvidence.fileUrl, title: 'Field Resolution Evidence' })}
                    />
                    <button
                      onClick={() => setActiveLightboxImg({ url: resolutionEvidence.fileUrl, title: 'Field Resolution Evidence' })}
                      className="absolute bottom-2 right-2 p-1.5 bg-black/60 hover:bg-black/80 text-white rounded text-[11px] flex items-center space-x-1 backdrop-blur-xs"
                      title="Inspect full resolution"
                    >
                      <Eye className="w-3.5 h-3.5" />
                      <span>Inspect</span>
                    </button>
                  </>
                ) : (
                  <div className="text-center p-6 text-[#66716E] space-y-2">
                    <Camera className="w-8 h-8 mx-auto text-[#66716E]/40" />
                    <p className="text-xs font-medium">No resolution photo submitted yet</p>
                    <p className="text-[10px] text-[#66716E] max-w-xs">
                      Field officer must upload photo evidence of completed remediation to trigger verification.
                    </p>
                    {(incident.status === 'IN_PROGRESS' || incident.status === 'ASSIGNED') && (
                      <button
                        onClick={onOpenResolutionModal}
                        className="mt-2 inline-flex items-center space-x-1 px-3 py-1.5 bg-[#163A36] text-white rounded text-xs font-bold hover:bg-[#2F6F68] transition"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Upload Resolution Photo</span>
                      </button>
                    )}
                  </div>
                )}
              </div>

              {/* Evidence Metadata Drawer */}
              <div className="p-4 space-y-2.5 text-xs font-mono bg-white flex-1 flex flex-col justify-between">
                <div className="space-y-1.5">
                  <div className="text-xs font-sans font-bold text-[#18201F]">
                    {resolution?.remarks || 'Awaiting field resolution submission'}
                  </div>
                  
                  <div className="flex items-center justify-between text-[#66716E] text-[11px] pt-1 border-t border-[#D9DEDB]/60">
                    <span className="flex items-center space-x-1">
                      <Clock className="w-3 h-3 text-[#66716E]" />
                      <span>Submitted:</span>
                    </span>
                    <span className="text-[#18201F] font-semibold">
                      {resolution?.createdAt ? new Date(resolution.createdAt).toLocaleString() : 'Pending'}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-[#66716E] text-[11px]">
                    <span className="flex items-center space-x-1">
                      <ShieldCheck className="w-3 h-3 text-[#2F6F68]" />
                      <span>Officer Attribution:</span>
                    </span>
                    <span className="text-[#18201F]">
                      {resolution?.officerName || (resolution?.officerId ? `Officer #${resolution.officerId}` : 'Not assigned')}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-[#66716E] text-[11px]">
                    <span className="flex items-center space-x-1">
                      <HardDrive className="w-3 h-3 text-[#66716E]" />
                      <span>Payload Size:</span>
                    </span>
                    <span className="text-[#18201F]">
                      {formatFileSize(resolutionEvidence?.fileSize)}
                    </span>
                  </div>
                </div>

                {/* SHA-256 Fingerprint */}
                <div className="bg-[#F5F4EF] p-2.5 rounded border border-[#D9DEDB] space-y-1 text-[10px]">
                  <div className="flex items-center justify-between text-[#66716E] font-bold">
                    <span className="flex items-center space-x-1">
                      <FileCheck className="w-3 h-3 text-[#2F6F68]" />
                      <span>SHA-256 File Checksum</span>
                    </span>
                    {resolutionEvidence?.contentHash && (
                      <button
                        onClick={() => copyToClipboard(resolutionEvidence.contentHash, 'res')}
                        className="text-[#2F6F68] hover:text-[#163A36] flex items-center space-x-0.5"
                        title="Copy cryptographic hash"
                      >
                        {copiedHash === 'res' ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                        <span>{copiedHash === 'res' ? 'Copied' : 'Copy'}</span>
                      </button>
                    )}
                  </div>
                  <div className="break-all font-mono text-[#18201F] select-all bg-white p-1 rounded border border-[#D9DEDB]/50">
                    {resolutionEvidence?.contentHash || 'Fingerprinted upon officer upload'}
                  </div>
                  <span className="text-[9px] text-[#66716E] block italic">
                    Prevents recycling pre-existing photos as new work proof.
                  </span>
                </div>
              </div>
            </div>
          )}

        </div>
      </Card>

      {/* 3. Resident Resolution Confirmation & Dispute Assessment */}
      <Card className="border-[#D9DEDB] space-y-3.5">
        <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2.5">
          <div className="flex items-center space-x-2">
            <MessageSquare className="w-4 h-4 text-[#163A36]" />
            <h3 className="text-xs font-bold text-[#18201F] uppercase tracking-wider">
              Resident Resolution Confirmation & Dispute Assessment
            </h3>
          </div>
          {feedbackList.length > 0 ? (
            <Badge variant={feedbackList[0].feedbackType === 'CONFIRMED' ? 'success' : 'danger'}>
              {feedbackList[0].feedbackType === 'CONFIRMED' ? 'RESOLUTION CONFIRMED' : 'DISPUTED BY RESIDENT'}
            </Badge>
          ) : (
            <Badge variant={incident.status === 'RESOLVED' ? 'warning' : 'default'}>
              {incident.status === 'RESOLVED' ? 'AWAITING RESIDENT FEEDBACK' : 'RESOLVED STATE REQUIRED'}
            </Badge>
          )}
        </div>

        {feedbackSuccessToast && (
          <div className="p-3 bg-[#3F7D5A]/10 border border-[#3F7D5A]/30 rounded-lg text-xs text-[#3F7D5A] flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{feedbackSuccessToast}</span>
            </div>
            <button 
              onClick={() => setFeedbackSuccessToast(null)} 
              className="text-[#3F7D5A] hover:text-[#18201F] font-bold text-xs ml-2"
            >
              ✕
            </button>
          </div>
        )}

        {/* If feedback already submitted */}
        {feedbackList.length > 0 ? (
          <div className="space-y-3">
            {feedbackList.map((fb) => (
              <div 
                key={fb.id} 
                className={`p-3.5 rounded-lg border text-xs space-y-2.5 ${
                  fb.feedbackType === 'CONFIRMED' 
                    ? 'bg-[#3F7D5A]/5 border-[#3F7D5A]/30' 
                    : 'bg-[#B94A48]/5 border-[#B94A48]/30'
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 border-b border-[#D9DEDB]/60 pb-2">
                  <div className="flex items-center space-x-2">
                    {fb.feedbackType === 'CONFIRMED' ? (
                      <ThumbsUp className="w-4 h-4 text-[#3F7D5A]" />
                    ) : (
                      <ThumbsDown className="w-4 h-4 text-[#B94A48]" />
                    )}
                    <span className="font-bold text-[#18201F]">
                      {fb.feedbackType === 'CONFIRMED' 
                        ? 'Resident Confirmed Satisfactory Resolution' 
                        : 'Resident Formally Disputed Resolution'}
                    </span>
                  </div>
                  <div className="flex items-center space-x-2 font-mono text-[10px] text-[#66716E]">
                    <span>Reporter: <strong>{fb.residentName || 'Resident'}</strong></span>
                    <span>•</span>
                    <span>{new Date(fb.createdAt).toLocaleString()}</span>
                  </div>
                </div>

                {fb.explanation && (
                  <div className="bg-white p-2.5 rounded border border-[#D9DEDB] space-y-1">
                    <span className="text-[10px] font-mono text-[#66716E] uppercase block">
                      {fb.feedbackType === 'CONFIRMED' ? 'Resident Feedback Remarks:' : 'Resident Dispute Justification:'}
                    </span>
                    <p className="text-[#18201F] text-xs leading-relaxed italic">
                      "{fb.explanation}"
                    </p>
                  </div>
                )}

                {/* Dispute Status & Officer Review details */}
                {fb.feedbackType === 'DISPUTED' && (
                  <div className="pt-1 space-y-2">
                    <div className="flex items-center justify-between text-[11px]">
                      <span className="font-mono text-[#66716E]">Dispute Workflow Status:</span>
                      <Badge variant={
                        fb.status === 'PENDING_REVIEW' ? 'warning' :
                        fb.status === 'ACCEPTED_FOR_REWORK' ? 'danger' :
                        fb.status === 'DISMISSED' ? 'success' : 'primary'
                      }>
                        {fb.status}
                      </Badge>
                    </div>

                    {fb.reviewed ? (
                      <div className="bg-white p-2.5 rounded border border-[#2F6F68]/30 space-y-1 text-xs">
                        <div className="flex items-center justify-between font-mono text-[10px] text-[#2F6F68] font-bold">
                          <span>Officer Review Action Logged</span>
                          <span>{fb.reviewedAt ? new Date(fb.reviewedAt).toLocaleString() : ''}</span>
                        </div>
                        <div className="text-[#18201F] text-[11px]">
                          Reviewed By: <strong>{fb.reviewedByOfficerName || `Officer #${fb.reviewedByOfficerId}`}</strong>
                        </div>
                        {fb.reviewNotes && (
                          <p className="text-[#18201F] text-[11px] bg-[#F5F4EF] p-2 rounded border border-[#D9DEDB] mt-1">
                            Review Notes: {fb.reviewNotes}
                          </p>
                        )}
                      </div>
                    ) : (
                      <div className="flex items-center justify-between bg-amber-50 p-2.5 rounded border border-amber-200">
                        <span className="text-[11px] text-amber-800">
                          Awaiting supervisory officer review and re-inspection dispatch.
                        </span>
                        {(currentUser?.role === 'OFFICER' || currentUser?.role === 'ADMIN') && (
                          <button
                            onClick={() => {
                              setSelectedFeedbackForReview(fb);
                              setReviewStatus('ACCEPTED_FOR_REWORK');
                              setReviewNotes('');
                              setReviewError(null);
                              setShowReviewModal(true);
                            }}
                            className="px-2.5 py-1 bg-[#163A36] text-white rounded text-[11px] font-bold hover:bg-[#2F6F68] transition"
                          >
                            Review Dispute
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          /* When no feedback has been submitted yet */
          <div className="space-y-3">
            {incident.status === 'RESOLVED' ? (
              <div className="p-4 bg-[#F5F4EF] rounded-lg border border-[#D9DEDB] space-y-3">
                <div className="space-y-1">
                  <h4 className="text-xs font-bold text-[#18201F] flex items-center space-x-1.5">
                    <UserCheck className="w-4 h-4 text-[#2F6F68]" />
                    <span>Resident Verification Gateway</span>
                  </h4>
                  <p className="text-xs text-[#66716E] leading-relaxed">
                    Municipal field operations have submitted remediation proof and marked this incident <strong>RESOLVED</strong>.
                    As the resident who reported this issue, please inspect the work and confirm whether it has been resolved to your satisfaction, or dispute the resolution with reasons if the issue persists.
                  </p>
                </div>

                {currentUser?.role === 'CITIZEN' ? (
                  <div className="flex flex-wrap items-center gap-2 pt-1 border-t border-[#D9DEDB]">
                    <button
                      onClick={() => {
                        setConfirmNotes('');
                        setConfirmError(null);
                        setShowConfirmModal(true);
                      }}
                      className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-[#3F7D5A] hover:bg-[#326447] text-white text-xs font-bold rounded transition shadow-xs"
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>Confirm Resolution</span>
                    </button>
                    <button
                      onClick={() => {
                        setDisputeExplanation('');
                        setDisputeError(null);
                        setShowDisputeModal(true);
                      }}
                      className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-[#B94A48] hover:bg-[#9E3937] text-white text-xs font-bold rounded transition shadow-xs"
                    >
                      <AlertTriangle className="w-3.5 h-3.5" />
                      <span>Dispute Resolution</span>
                    </button>
                  </div>
                ) : (
                  <div className="p-2.5 bg-white rounded border border-[#D9DEDB] text-xs text-[#66716E] flex items-center space-x-2">
                    <Info className="w-4 h-4 text-[#C58A32] shrink-0" />
                    <span>
                      Only the authenticated original resident reporter can submit resolution feedback. Officers and administrators cannot submit feedback or impersonate residents on this endpoint.
                    </span>
                  </div>
                )}
              </div>
            ) : (
              <div className="p-3.5 bg-[#F5F4EF] rounded border border-[#D9DEDB] text-xs text-[#66716E] flex items-center space-x-2">
                <Info className="w-4 h-4 text-[#66716E] shrink-0" />
                <span>
                  Resident resolution confirmation and dispute actions will activate once the incident is moved to <strong>RESOLVED</strong> status.
                </span>
              </div>
            )}
          </div>
        )}
      </Card>

      {/* 4. System Integrity & Verification Boundaries Ledger */}
      <Card className="border-[#D9DEDB] space-y-3 bg-[#F5F4EF]/50">
        <div className="flex items-center space-x-2 border-b border-[#D9DEDB] pb-2.5">
          <ShieldCheck className="w-4 h-4 text-[#163A36]" />
          <h3 className="text-xs font-bold text-[#18201F] uppercase tracking-wider">
            Verification Protocol & System Boundaries
          </h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
          
          {/* Genuinely Verified by the System */}
          <div className="bg-white p-3.5 rounded border border-[#3F7D5A]/30 space-y-2">
            <div className="flex items-center space-x-2 text-[#3F7D5A] font-bold text-xs uppercase font-mono">
              <CheckCircle2 className="w-4 h-4" />
              <span>Genuinely Verified by System</span>
            </div>
            <ul className="space-y-1.5 text-[#18201F] list-disc list-inside text-[11px] leading-relaxed">
              <li>
                <strong>Spatial Proximity:</strong> Haversine formula calculation confirms resolution capture is within 500 meters of reported coordinates (60% weight).
              </li>
              <li>
                <strong>Payload Telemetry:</strong> Binary byte volume correlation confirms realistic file submission (40% weight).
              </li>
              <li>
                <strong>SHA-256 Deduplication:</strong> Ingestion engine hashes every file with SHA-256 and rejects cross-incident duplicates.
              </li>
              <li>
                <strong>Role Authorization:</strong> Backend enforces that only authenticated Officers and Admins can submit resolution evidence.
              </li>
            </ul>
          </div>

          {/* Requires Human / Field Verification */}
          <div className="bg-white p-3.5 rounded border border-[#C58A32]/30 space-y-2">
            <div className="flex items-center space-x-2 text-[#C58A32] font-bold text-xs uppercase font-mono">
              <AlertTriangle className="w-4 h-4" />
              <span>Requires Human / Field Verification</span>
            </div>
            <ul className="space-y-1.5 text-[#18201F] list-disc list-inside text-[11px] leading-relaxed">
              <li>
                <strong>Physical Workmanship:</strong> Pavement compaction, drainage clearance, or structural integrity cannot be judged by hash match alone.
              </li>
              <li>
                <strong>Scene Authenticity:</strong> A proximity match does not prove the photograph depicts the exact physical spot or that the repair is physically complete.
              </li>
              <li>
                <strong>Long-Term Remediation:</strong> Field supervisor must confirm that root cause failure is resolved and will not recur.
              </li>
            </ul>
          </div>

        </div>
      </Card>

      {/* 4. Chronological Workflow Audit Trail */}
      <Card className="border-[#D9DEDB] space-y-3">
        <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-2.5">
          <div className="flex items-center space-x-2">
            <Clock className="w-4 h-4 text-[#C58A32]" />
            <h3 className="text-xs font-bold text-[#18201F] uppercase tracking-wider">
              Chronological Workflow Audit Trail ({actions.length} Events)
            </h3>
          </div>
          <span className="font-mono text-[10px] text-[#66716E]">
            Immutable Transition Ledger
          </span>
        </div>

        {actions.length === 0 ? (
          <div className="text-center py-6 text-xs text-[#66716E]">
            No workflow actions recorded yet. Incident is awaiting initial assignment.
          </div>
        ) : (
          <div className="relative pl-6 space-y-3 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-[#D9DEDB]">
            {actions.map((act, idx) => {
              const actionVariant = 
                act.actionType === 'RESOLVED' || act.actionType === 'VERIFIED' || act.actionType === 'RESIDENT_CONFIRMED' ? 'success' :
                act.actionType === 'STARTED' || act.actionType === 'DISPUTE_REVIEWED' ? 'primary' :
                act.actionType === 'REOPENED' || act.actionType === 'RESIDENT_DISPUTED' ? 'danger' : 'warning';

              return (
                <div key={act.id || idx} className="relative group">
                  {/* Dot */}
                  <div className={`absolute -left-6 top-1.5 w-2.5 h-2.5 rounded-full border-2 border-white ${
                    act.actionType === 'RESOLVED' || act.actionType === 'VERIFIED' || act.actionType === 'RESIDENT_CONFIRMED' ? 'bg-[#3F7D5A]' :
                    act.actionType === 'REOPENED' || act.actionType === 'RESIDENT_DISPUTED' ? 'bg-[#B94A48]' :
                    act.actionType === 'STARTED' || act.actionType === 'DISPUTE_REVIEWED' ? 'bg-[#163A36]' : 'bg-[#C58A32]'
                  }`} />

                  <div className="bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] space-y-1.5">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
                      <div className="flex items-center space-x-2">
                        <Badge variant={actionVariant}>{act.actionType}</Badge>
                        <span className="text-xs font-bold text-[#18201F]">
                          {act.officerName || (act.officerId ? `Officer #${act.officerId}` : 'Municipal Officer')}
                        </span>
                      </div>
                      <span className="font-mono text-[10px] text-[#66716E]">
                        {act.createdAt ? new Date(act.createdAt).toLocaleString() : 'Just now'}
                      </span>
                    </div>

                    <p className="text-xs text-[#18201F] leading-relaxed">
                      {act.remarks || 'Standard workflow status transition.'}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>

      {/* Lightbox Modal */}
      {activeLightboxImg && (
        <div 
          className="fixed inset-0 z-[2000] bg-black/80 backdrop-blur-sm flex items-center justify-center p-4"
          onClick={() => setActiveLightboxImg(null)}
        >
          <div 
            className="bg-white rounded-lg max-w-4xl max-h-[90vh] overflow-hidden flex flex-col shadow-2xl border border-[#D9DEDB]"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-3 border-b border-[#D9DEDB] flex items-center justify-between bg-[#F5F4EF]">
              <span className="font-bold text-xs text-[#18201F]">{activeLightboxImg.title}</span>
              <button 
                onClick={() => setActiveLightboxImg(null)}
                className="text-[#66716E] hover:text-[#18201F] font-bold text-sm px-2 py-0.5"
              >
                ✕
              </button>
            </div>
            <div className="p-2 bg-black flex items-center justify-center overflow-auto max-h-[75vh]">
              <img 
                src={activeLightboxImg.url} 
                alt="Enlarged evidence" 
                className="max-h-[75vh] w-auto object-contain rounded"
              />
            </div>
          </div>
        </div>
      )}

      {/* Modal: Confirm Resolution */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white border border-[#D9DEDB] rounded-lg max-w-lg w-full p-5 space-y-4 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <CheckCircle2 className="w-5 h-5 text-[#3F7D5A]" />
                <h3 className="text-base font-bold text-[#18201F]">Confirm Civic Resolution</h3>
              </div>
              <button 
                type="button" 
                onClick={() => setShowConfirmModal(false)} 
                className="text-[#66716E] hover:text-[#18201F] font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-[#66716E] leading-relaxed">
              Confirming indicates that the civic issue at <strong>{incident.address}</strong> has been satisfactorily resolved by the municipal field unit. This will record an immutable confirmation event in the audit ledger.
            </p>

            {confirmError && (
              <div className="p-2.5 bg-[#B94A48]/10 border border-[#B94A48]/30 rounded text-xs text-[#B94A48]">
                {confirmError}
              </div>
            )}

            <div className="space-y-1.5 text-xs">
              <label className="block font-bold text-[#18201F]">
                Optional Resident Remarks:
              </label>
              <textarea
                rows={2}
                value={confirmNotes}
                onChange={(e) => setConfirmNotes(e.target.value)}
                maxLength={500}
                placeholder="Optional confirmation comments (e.g. Verified on site, water flowing freely now)..."
                className="w-full bg-[#F5F4EF] border border-[#D9DEDB] rounded p-2.5 text-xs text-[#18201F] focus:outline-none focus:border-[#3F7D5A]"
              />
            </div>

            <div className="pt-2 border-t border-[#D9DEDB] flex justify-end space-x-2">
              <button
                type="button"
                onClick={() => setShowConfirmModal(false)}
                className="px-3 py-1.5 bg-[#F5F4EF] hover:bg-[#e0ded8] text-[#18201F] text-xs font-bold rounded"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={submittingFeedback}
                onClick={handleConfirmResolution}
                className="px-3 py-1.5 bg-[#3F7D5A] hover:bg-[#326447] text-white text-xs font-bold rounded transition disabled:opacity-50 flex items-center space-x-1"
              >
                {submittingFeedback ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
                <span>{submittingFeedback ? 'Submitting...' : 'Confirm Satisfied'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Dispute Resolution */}
      {showDisputeModal && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <form onSubmit={handleDisputeResolution} className="bg-white border border-[#D9DEDB] rounded-lg max-w-lg w-full p-5 space-y-4 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <AlertTriangle className="w-5 h-5 text-[#B94A48]" />
                <h3 className="text-base font-bold text-[#18201F]">Dispute Resolution</h3>
              </div>
              <button 
                type="button" 
                onClick={() => setShowDisputeModal(false)} 
                className="text-[#66716E] hover:text-[#18201F] font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-[#66716E] leading-relaxed">
              If the reported issue persists or the repair was inadequate, please submit a detailed explanation. This action will transition the incident back into the municipal review queue and require an authorized officer to investigate.
            </p>

            {disputeError && (
              <div className="p-2.5 bg-[#B94A48]/10 border border-[#B94A48]/30 rounded text-xs text-[#B94A48]">
                {disputeError}
              </div>
            )}

            <div className="space-y-1.5 text-xs">
              <div className="flex justify-between items-center">
                <label className="block font-bold text-[#18201F]">
                  Mandatory Dispute Explanation: <span className="text-[#B94A48]">*</span>
                </label>
                <span className={`text-[10px] font-mono ${disputeExplanation.length > 1900 ? 'text-[#B94A48]' : 'text-[#66716E]'}`}>
                  {disputeExplanation.length} / 2000
                </span>
              </div>
              <textarea
                rows={4}
                required
                value={disputeExplanation}
                onChange={(e) => setDisputeExplanation(e.target.value)}
                maxLength={2000}
                placeholder="Explain clearly why the resolution is incomplete (e.g. Silt was excavated but left on sidewalk; drain inlet is still blocked; water backed up again after rain)..."
                className="w-full bg-[#F5F4EF] border border-[#D9DEDB] rounded p-2.5 text-xs text-[#18201F] focus:outline-none focus:border-[#B94A48]"
              />
            </div>

            <div className="pt-2 border-t border-[#D9DEDB] flex justify-end space-x-2">
              <button
                type="button"
                onClick={() => setShowDisputeModal(false)}
                className="px-3 py-1.5 bg-[#F5F4EF] hover:bg-[#e0ded8] text-[#18201F] text-xs font-bold rounded"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submittingFeedback || !disputeExplanation.trim()}
                className="px-3 py-1.5 bg-[#B94A48] hover:bg-[#9E3937] text-white text-xs font-bold rounded transition disabled:opacity-50 flex items-center space-x-1"
              >
                {submittingFeedback ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <AlertTriangle className="w-3.5 h-3.5" />}
                <span>{submittingFeedback ? 'Filing Dispute...' : 'Submit Formal Dispute'}</span>
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Modal: Officer Dispute Review */}
      {showReviewModal && selectedFeedbackForReview && (
        <div className="fixed inset-0 z-[2000] bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <form onSubmit={handleReviewDispute} className="bg-white border border-[#D9DEDB] rounded-lg max-w-lg w-full p-5 space-y-4 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <ShieldCheck className="w-5 h-5 text-[#163A36]" />
                <h3 className="text-base font-bold text-[#18201F]">Conduct Officer Dispute Review</h3>
              </div>
              <button 
                type="button" 
                onClick={() => setShowReviewModal(false)} 
                className="text-[#66716E] hover:text-[#18201F] font-bold text-lg"
              >
                ✕
              </button>
            </div>

            <div className="bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] space-y-1 text-xs">
              <span className="font-mono text-[10px] text-[#66716E] uppercase block">Resident Justification Under Review:</span>
              <p className="text-[#18201F] italic">"{selectedFeedbackForReview.explanation}"</p>
            </div>

            {reviewError && (
              <div className="p-2.5 bg-[#B94A48]/10 border border-[#B94A48]/30 rounded text-xs text-[#B94A48]">
                {reviewError}
              </div>
            )}

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-[#18201F] mb-1">
                  Dispute Determination / Status:
                </label>
                <select
                  value={reviewStatus}
                  onChange={(e) => setReviewStatus(e.target.value)}
                  className="w-full bg-[#F5F4EF] border border-[#D9DEDB] rounded p-2 text-xs text-[#18201F]"
                >
                  <option value="ACCEPTED_FOR_REWORK">ACCEPTED_FOR_REWORK — Valid dispute; re-work ordered</option>
                  <option value="UNDER_SUPERVISION">UNDER_SUPERVISION — Assigned for supervisory on-site inspection</option>
                  <option value="DISMISSED">DISMISSED — Reviewed and determined completed to spec</option>
                </select>
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="block font-bold text-[#18201F]">
                    Officer Review Notes & Instructions:
                  </label>
                  <span className="text-[10px] font-mono text-[#66716E]">{reviewNotes.length} / 2000</span>
                </div>
                <textarea
                  rows={3}
                  value={reviewNotes}
                  onChange={(e) => setReviewNotes(e.target.value)}
                  maxLength={2000}
                  placeholder="Record supervisory notes and instructions for field unit..."
                  className="w-full bg-[#F5F4EF] border border-[#D9DEDB] rounded p-2.5 text-xs text-[#18201F] focus:outline-none focus:border-[#163A36]"
                />
              </div>
            </div>

            <div className="pt-2 border-t border-[#D9DEDB] flex justify-end space-x-2">
              <button
                type="button"
                onClick={() => setShowReviewModal(false)}
                className="px-3 py-1.5 bg-[#F5F4EF] hover:bg-[#e0ded8] text-[#18201F] text-xs font-bold rounded"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submittingFeedback}
                className="px-3 py-1.5 bg-[#163A36] hover:bg-[#2F6F68] text-white text-xs font-bold rounded transition disabled:opacity-50 flex items-center space-x-1"
              >
                {submittingFeedback ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                <span>{submittingFeedback ? 'Recording...' : 'Submit Review'}</span>
              </button>
            </div>
          </form>
        </div>
      )}

    </div>
  );
};
