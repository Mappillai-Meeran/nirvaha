import React, { useState, useEffect } from 'react';
import { 
  Clock, 
  AlertTriangle, 
  CheckCircle2, 
  ShieldCheck, 
  RotateCcw, 
  Info, 
  ChevronRight, 
  Bell, 
  ExternalLink,
  Filter,
  FileText,
  Calendar,
  AlertCircle
} from 'lucide-react';
import { Badge, Card, LoadingState, ErrorState } from '../ui/index.jsx';
import { slaService } from '../../services/index.js';

export const SlaTrackingPanel = ({ onSelectIncident = null }) => {
  const [loading, setLoading] = useState(true);
  const [evaluating, setEvaluating] = useState(false);
  const [error, setError] = useState(null);
  const [summary, setSummary] = useState({
    complianceRate: 100,
    withinStandardCount: 0,
    approachingCount: 0,
    overdueCount: 0,
    completedCount: 0,
    totalTracked: 0
  });
  const [trackingList, setTrackingList] = useState([]);
  const [rulesList, setRulesList] = useState([]);
  const [activeTab, setActiveTab] = useState('ALL');
  const [showRulesModal, setShowRulesModal] = useState(false);
  const [lastRefreshed, setLastRefreshed] = useState(new Date());

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [summaryRes, statusRes, rulesRes] = await Promise.all([
        slaService.getSummary().catch(() => ({})),
        slaService.getStatus().catch(() => []),
        slaService.getRules().catch(() => [])
      ]);

      if (summaryRes) setSummary(summaryRes);
      if (Array.isArray(statusRes)) setTrackingList(statusRes);
      if (Array.isArray(rulesRes)) setRulesList(rulesRes);
      setLastRefreshed(new Date());
    } catch (err) {
      console.error('Failed to load SLA tracking data:', err);
      setError('Unable to load statutory SLA tracking telemetry.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleEvaluateNow = async () => {
    try {
      setEvaluating(true);
      await slaService.evaluate();
      await fetchData();
    } catch (err) {
      console.error('Failed to trigger SLA evaluation cycle:', err);
    } finally {
      setEvaluating(false);
    }
  };

  const filteredTracking = trackingList.filter((item) => {
    if (activeTab === 'OFFICIAL') return item.isOfficialSla;
    if (activeTab === 'APPROACHING') return item.slaStatus === 'APPROACHING_DEADLINE';
    if (activeTab === 'OVERDUE') return item.slaStatus === 'OVERDUE';
    if (activeTab === 'INTERNAL') return !item.isOfficialSla;
    if (activeTab === 'COMPLETED') return item.slaStatus === 'COMPLETED';
    return true;
  });

  return (
    <div className="space-y-4">
      {/* Top Banner / Control Bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-white p-4 rounded-lg border border-[#D9DEDB] shadow-xs">
        <div className="space-y-1">
          <div className="flex items-center space-x-2">
            <span className="p-1.5 bg-[#163A36]/10 text-[#163A36] rounded-md">
              <ShieldCheck className="w-5 h-5 text-[#163A36]" />
            </span>
            <h2 className="text-base font-bold text-[#18201F] tracking-tight">
              Statutory Service Rule & SLA Engine
            </h2>
          </div>
          <p className="text-xs text-[#66716E]">
            Auditable tracking against Puducherry statutory Citizen Charters. Proposed operational targets held separate from official commitments.
          </p>
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto justify-end">
          <button
            onClick={() => setShowRulesModal(true)}
            className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-[#163A36] bg-[#F5F4EF] hover:bg-[#EAE8E0] border border-[#D9DEDB] rounded-md transition"
            title="Inspect statutory charter rules"
          >
            <FileText className="w-3.5 h-3.5 mr-1.5" />
            Charter Reference
          </button>
          <button
            onClick={handleEvaluateNow}
            disabled={evaluating}
            className="inline-flex items-center px-3 py-1.5 text-xs font-bold text-white bg-[#163A36] hover:bg-[#1f4c47] rounded-md transition disabled:opacity-50 shadow-xs"
          >
            <RotateCcw className={`w-3.5 h-3.5 mr-1.5 ${evaluating ? 'animate-spin' : ''}`} />
            {evaluating ? 'Evaluating...' : 'Evaluate Now'}
          </button>
        </div>
      </div>

      {/* KPI Cards Row */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        {/* Card 1: Statutory Compliance Rate */}
        <div className="bg-white p-3.5 rounded-lg border border-[#D9DEDB] shadow-xs space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-[#66716E]">SLA Compliance</span>
            <ShieldCheck className="w-4 h-4 text-[#3F7D5A]" />
          </div>
          <div className="text-2xl font-black font-mono text-[#3F7D5A]">
            {summary.complianceRate != null ? `${summary.complianceRate}%` : '100%'}
          </div>
          <span className="text-[10px] text-[#66716E] block truncate">
            Official statutory rate
          </span>
        </div>

        {/* Card 2: Within Standard */}
        <div className="bg-white p-3.5 rounded-lg border border-[#D9DEDB] shadow-xs space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-[#66716E]">Within Standard</span>
            <CheckCircle2 className="w-4 h-4 text-[#2F6F68]" />
          </div>
          <div className="text-2xl font-black font-mono text-[#2F6F68]">
            {summary.withinStandardCount || 0}
          </div>
          <span className="text-[10px] text-[#2F6F68] font-medium block">
            On track to meet SLA
          </span>
        </div>

        {/* Card 3: Approaching Deadline */}
        <div className="bg-white p-3.5 rounded-lg border border-[#D9DEDB] shadow-xs space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-[#66716E]">Approaching Target</span>
            <Clock className="w-4 h-4 text-[#C58A32]" />
          </div>
          <div className="text-2xl font-black font-mono text-[#C58A32]">
            {summary.approachingCount || 0}
          </div>
          <span className="text-[10px] text-[#C58A32] font-medium block">
            ≤ Warning threshold
          </span>
        </div>

        {/* Card 4: Service Target Exceeded */}
        <div className="bg-white p-3.5 rounded-lg border border-[#D9DEDB] shadow-xs space-y-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-[#66716E]">Target Exceeded</span>
            <AlertTriangle className="w-4 h-4 text-[#B94A48]" />
          </div>
          <div className="text-2xl font-black font-mono text-[#B94A48]">
            {summary.overdueCount || 0}
          </div>
          <span className="text-[10px] text-[#B94A48] font-medium block">
            Official SLA overdue
          </span>
        </div>

        {/* Card 5: Total Tracked */}
        <div className="bg-white p-3.5 rounded-lg border border-[#D9DEDB] shadow-xs space-y-1 col-span-2 md:col-span-1">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-[#66716E]">Total Tracked</span>
            <Calendar className="w-4 h-4 text-[#66716E]" />
          </div>
          <div className="text-2xl font-black font-mono text-[#18201F]">
            {summary.totalTracked || trackingList.length}
          </div>
          <span className="text-[10px] text-[#66716E] block">
            Scheduled cycle: 6h
          </span>
        </div>
      </div>

      {/* Main Panel Content */}
      <Card className="space-y-4">
        {/* Tab Controls & Legend */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 border-b border-[#D9DEDB] pb-3">
          <div className="flex items-center space-x-1 overflow-x-auto w-full sm:w-auto pb-1 sm:pb-0">
            {[
              { key: 'ALL', label: `All (${trackingList.length})` },
              { key: 'OFFICIAL', label: 'Official SLAs' },
              { key: 'APPROACHING', label: `Approaching (${summary.approachingCount || 0})` },
              { key: 'OVERDUE', label: `Target Exceeded (${summary.overdueCount || 0})` },
              { key: 'INTERNAL', label: 'Internal Targets' },
              { key: 'COMPLETED', label: 'Completed' }
            ].map(tab => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`px-3 py-1 rounded-md text-xs font-semibold transition whitespace-nowrap ${
                  activeTab === tab.key
                    ? 'bg-[#163A36] text-white shadow-xs'
                    : 'text-[#66716E] hover:bg-[#F5F4EF] hover:text-[#18201F]'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className="flex items-center space-x-3 text-[11px] text-[#66716E]">
            <span className="flex items-center space-x-1">
              <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block"></span>
              <span>Official SLA</span>
            </span>
            <span className="flex items-center space-x-1">
              <span className="w-2 h-2 rounded-full bg-amber-500 inline-block"></span>
              <span>Proposed Target</span>
            </span>
          </div>
        </div>

        {/* Tracking List Content */}
        {loading ? (
          <LoadingState message="Evaluating statutory service rules and deadlines..." />
        ) : error ? (
          <ErrorState message={error} onRetry={fetchData} />
        ) : filteredTracking.length === 0 ? (
          <div className="py-12 text-center text-[#66716E] space-y-2">
            <CheckCircle2 className="w-8 h-8 text-[#3F7D5A] mx-auto opacity-60" />
            <p className="text-xs font-semibold text-[#18201F]">No incidents matching this SLA filter</p>
            <p className="text-[11px] text-[#66716E]">All tracked civic cases are compliant or no cases exist in this category.</p>
          </div>
        ) : (
          <div className="space-y-2.5">
            {filteredTracking.map((item) => {
              const isOverdue = item.slaStatus === 'OVERDUE';
              const isApproaching = item.slaStatus === 'APPROACHING_DEADLINE';
              const isWithin = item.slaStatus === 'WITHIN_STANDARD';
              const isCompleted = item.slaStatus === 'COMPLETED';

              const targetDays = item.targetDays || 15;
              const elapsedDays = item.elapsedDays != null ? item.elapsedDays : 0;
              const progressPct = Math.min(100, Math.round((elapsedDays / targetDays) * 100));

              return (
                <div
                  key={item.id}
                  onClick={() => onSelectIncident && item.incidentId && onSelectIncident(item.incidentId)}
                  className={`p-3.5 rounded-lg border transition hover:border-[#163A36] hover:shadow-xs cursor-pointer ${
                    isOverdue
                      ? 'bg-[#B94A48]/5 border-[#B94A48]/30'
                      : isApproaching
                      ? 'bg-[#C58A32]/5 border-[#C58A32]/30'
                      : 'bg-white border-[#D9DEDB]'
                  }`}
                >
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-2.5">
                    {/* Left: Incident info & classification */}
                    <div className="space-y-1 md:max-w-[45%]">
                      <div className="flex items-center space-x-2 flex-wrap gap-1">
                        <span className="font-mono text-xs font-bold text-[#163A36]">
                          #{item.incidentId}
                        </span>
                        <span className="text-xs font-semibold text-[#18201F] truncate">
                          {item.incidentTitle || 'Civic Grievance'}
                        </span>
                        {item.isOfficialSla ? (
                          <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-800 border border-emerald-300">
                            OFFICIAL SLA
                          </span>
                        ) : (
                          <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 border border-amber-300">
                            PROPOSED TARGET
                          </span>
                        )}
                        {item.wardName && (
                          <span className="text-[10px] text-[#66716E] font-mono">
                            • {item.wardName}
                          </span>
                        )}
                      </div>

                      <div className="text-[11px] text-[#66716E] flex items-center space-x-2">
                        <span>{item.department || 'Municipal Dept'}</span>
                        <span>•</span>
                        <span className="italic truncate">{item.serviceDescription}</span>
                      </div>
                    </div>

                    {/* Middle: Progress bar against target */}
                    <div className="space-y-1 md:w-48">
                      <div className="flex justify-between text-[10px] font-mono">
                        <span className="text-[#66716E]">{elapsedDays}d elapsed</span>
                        <span className="font-bold text-[#18201F]">{targetDays}d target</span>
                      </div>
                      <div className="w-full bg-[#EAE8E0] h-1.5 rounded-full overflow-hidden">
                        <div
                          className={`h-full transition-all duration-500 rounded-full ${
                            isOverdue
                              ? 'bg-[#B94A48]'
                              : isApproaching
                              ? 'bg-[#C58A32]'
                              : isCompleted
                              ? 'bg-[#3F7D5A]'
                              : 'bg-[#2F6F68]'
                          }`}
                          style={{ width: `${progressPct}%` }}
                        />
                      </div>
                      <div className="text-[9px] text-[#66716E] font-mono text-right">
                        {item.statutoryReference ? item.statutoryReference : 'Citizen Charter'}
                      </div>
                    </div>

                    {/* Right: Neutral Status Badge & Escalation */}
                    <div className="flex items-center justify-between md:justify-end space-x-2 shrink-0">
                      {isOverdue ? (
                        <div className="text-right">
                          <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-bold bg-[#B94A48]/10 text-[#B94A48] border border-[#B94A48]/30">
                            <AlertTriangle className="w-3.5 h-3.5 mr-1" />
                            Service Target Exceeded
                          </span>
                          <span className="block text-[10px] font-mono text-[#B94A48] mt-0.5">
                            {item.remainingDays != null ? `${Math.abs(item.remainingDays)}d overdue` : 'Past deadline'}
                          </span>
                        </div>
                      ) : isApproaching ? (
                        <div className="text-right">
                          <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-bold bg-[#C58A32]/10 text-[#C58A32] border border-[#C58A32]/30">
                            <Clock className="w-3.5 h-3.5 mr-1" />
                            Approaching Target
                          </span>
                          <span className="block text-[10px] font-mono text-[#C58A32] mt-0.5">
                            {item.remainingDays != null ? `${item.remainingDays}d remaining` : 'Warning window'}
                          </span>
                        </div>
                      ) : isCompleted ? (
                        <div className="text-right">
                          <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-bold bg-[#3F7D5A]/10 text-[#3F7D5A] border border-[#3F7D5A]/30">
                            <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                            Resolved in SLA
                          </span>
                        </div>
                      ) : !item.isOfficialSla ? (
                        <div className="text-right">
                          <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-bold bg-slate-100 text-slate-700 border border-slate-300">
                            Proposed Target
                          </span>
                          <span className="block text-[10px] font-mono text-slate-500 mt-0.5">
                            {targetDays}d operational
                          </span>
                        </div>
                      ) : (
                        <div className="text-right">
                          <span className="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-300">
                            <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
                            Within Standard
                          </span>
                          <span className="block text-[10px] font-mono text-emerald-600 mt-0.5">
                            {item.remainingDays != null ? `${item.remainingDays}d remaining` : 'On track'}
                          </span>
                        </div>
                      )}

                      {/* Escalation Level indicator */}
                      {item.escalationLevel > 0 && (
                        <span
                          className={`p-1.5 rounded-full ${
                            item.escalationLevel === 2
                              ? 'bg-rose-100 text-rose-700'
                              : 'bg-amber-100 text-amber-700'
                          }`}
                          title={`Escalation Level ${item.escalationLevel}: In-app notification dispatched to reporting resident`}
                        >
                          <Bell className="w-3.5 h-3.5" />
                        </span>
                      )}

                      <ChevronRight className="w-4 h-4 text-[#66716E]" />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>

      {/* Modal: Statutory Citizen Charter Reference */}
      {showRulesModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-lg border border-[#D9DEDB] max-w-2xl w-full p-5 space-y-4 shadow-xl">
            <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-3">
              <div className="flex items-center space-x-2">
                <FileText className="w-5 h-5 text-[#163A36]" />
                <h3 className="text-sm font-bold text-[#18201F]">
                  Puducherry UT — Statutory Citizen Charter Standards
                </h3>
              </div>
              <button
                onClick={() => setShowRulesModal(false)}
                className="text-xs text-[#66716E] hover:text-[#18201F] font-bold px-2 py-1"
              >
                ✕ Close
              </button>
            </div>

            <div className="space-y-3 max-h-96 overflow-y-auto text-xs pr-1">
              {rulesList.map((rule) => (
                <div
                  key={rule.id}
                  className={`p-3 rounded-md border ${
                    rule.isOfficialSla
                      ? 'bg-emerald-50/50 border-emerald-200'
                      : 'bg-[#F5F4EF] border-[#D9DEDB]'
                  }`}
                >
                  <div className="flex justify-between items-start gap-2">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="font-bold text-[#18201F]">{rule.department}</span>
                        <span className="text-[#66716E]">({rule.serviceCategory})</span>
                        {rule.isOfficialSla ? (
                          <span className="text-[9px] bg-emerald-100 text-emerald-800 font-bold px-1.5 py-0.5 rounded border border-emerald-300">
                            OFFICIAL SLA
                          </span>
                        ) : (
                          <span className="text-[9px] bg-amber-100 text-amber-800 font-bold px-1.5 py-0.5 rounded border border-amber-300">
                            PROPOSED TARGET
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-[#18201F] mt-1 leading-relaxed">
                        {rule.serviceDescription}
                      </p>
                    </div>

                    <div className="text-right shrink-0">
                      <span className="font-mono font-bold text-sm text-[#18201F]">
                        {rule.targetDays} Days
                      </span>
                      <span className="block text-[10px] text-[#66716E]">
                        Warning: {rule.warningThresholdDays || 3}d
                      </span>
                    </div>
                  </div>

                  <div className="mt-2 pt-2 border-t border-[#D9DEDB]/60 text-[10px] text-[#66716E] flex justify-between">
                    <span>Reference: <strong>{rule.statutoryReference || 'Standard'}</strong></span>
                    <span>Status: {rule.verificationStatus}</span>
                  </div>
                </div>
              ))}
            </div>

            <div className="text-[11px] text-[#66716E] bg-[#F5F4EF] p-2.5 rounded border border-[#D9DEDB] leading-relaxed">
              <strong>Audit Notice:</strong> Under Phase 5B specifications, only officially published citizen charter standards produce official SLA breach/overdue metrics. Internal operational targets are held separate to maintain audit integrity.
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
