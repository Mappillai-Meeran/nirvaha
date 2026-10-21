import React, { useEffect, useState } from 'react';
import { integrityService } from '../../services';
import { Card, Badge, LoadingState, ErrorState, EmptyState } from '../../components/ui';
import { ShieldCheck, ArrowRight, Info, RefreshCcw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const IntegrityRadar = () => {
  const navigate = useNavigate();
  const [anomalies, setAnomalies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAnomalies = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await integrityService.getOpenAnomalies();
      setAnomalies(data || []);
    } catch (err) {
      if (err?.status === 401 || err?.status === 403) {
        setError('Unauthorized: Officer or Administrator privileges are required to view integrity review telemetry. Please sign in with government credentials.');
      } else {
        setError(err.message || 'Failed to sync integrity radar telemetry');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    integrityService.getOpenAnomalies()
      .then((data) => {
        if (isMounted) setAnomalies(data || []);
      })
      .catch((err) => {
        if (isMounted) {
          if (err?.status === 401 || err?.status === 403) {
            setError('Unauthorized: Officer or Administrator privileges are required to view integrity review telemetry. Please sign in with government credentials.');
          } else {
            setError(err.message || 'Failed to sync integrity radar telemetry');
          }
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => { isMounted = false; };
  }, []);

  if (loading) return <LoadingState message="Scanning quality assurance & integrity telemetry..." />;
  if (error) return <ErrorState message={error} onRetry={fetchAnomalies} />;

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
      {/* Policy Framing Header */}
      <div className="bg-[#174f4a]/10 border border-[#174f4a]/30 rounded-lg p-3.5 text-xs text-[#174f4a] flex items-start space-x-2.5 shadow-xs">
        <Info className="w-4 h-4 text-[#174f4a] shrink-0 mt-0.5" />
        <div className="space-y-1">
          <strong className="block text-[#174f4a]">Municipal Quality Assurance & Evidence Audit Protocol:</strong>
          <p className="leading-relaxed text-[#3b4946]">
            This radar detects operational patterns requiring supervisory verification—such as evidence photo reuse, abnormal resolution turnarounds, or sudden velocity shifts. These flags represent routine quality control checkpoints designed to safeguard public trust, not accusations of misconduct.
          </p>
        </div>
      </div>

      <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-[#18201F] flex items-center space-x-2">
            <ShieldCheck className="w-5 h-5 text-[#3F7D5A]" />
            <span>Operational Quality & Integrity Review Radar</span>
          </h1>
          <p className="text-xs text-[#66716E] mt-0.5">
            Supervisory review recommendations flagged for evidence provenance and SLA compliance verification.
          </p>
        </div>

        <button
          onClick={fetchAnomalies}
          className="px-3 py-1.5 bg-white border border-[#D9DEDB] text-xs font-bold text-[#174f4a] rounded hover:bg-[#F5F4EF] flex items-center space-x-1.5 shadow-xs"
        >
          <RefreshCcw size={13} />
          <span>Refresh Audit</span>
        </button>
      </div>

      {anomalies.length === 0 ? (
        <EmptyState
          title="System Quality Assurance Nominal"
          description="Zero unflagged operational pattern flags detected across active municipal cases. All resolutions remain verified and compliant with municipal standards."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {anomalies.map((ano) => (
            <Card key={ano.id} className="space-y-3.5 border-[#D9DEDB] bg-white shadow-sm">
              <div className="flex items-start justify-between">
                <div>
                  <span className="font-mono text-xs text-[#174f4a] font-bold">CASE #{ano.incidentId || ano.id}</span>
                  <h3 className="font-bold text-sm text-[#18201F] mt-0.5">
                    {ano.anomalyType || 'OPERATIONAL PATTERN FLAG'}
                  </h3>
                </div>
                <Badge variant="warning">
                  REVIEW RECOMMENDED
                </Badge>
              </div>

              <div className="bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] space-y-1">
                <span className="text-[10px] font-mono text-[#66716E] uppercase font-bold block">
                  Quality Audit Observation:
                </span>
                <p className="text-xs text-[#18201F] leading-relaxed">
                  {ano.description || ano.reason || 'Pattern flagged for supervisory review before final archive closure.'}
                </p>
              </div>

              <div className="flex items-center justify-between text-[11px] font-mono text-[#66716E] pt-2 border-t border-[#D9DEDB]">
                <span>Status: Pending Supervisory Verification</span>
                <button
                  onClick={() => navigate(`/incidents/${ano.incidentId || ano.id}`)}
                  className="text-xs text-[#174f4a] hover:underline font-bold flex items-center space-x-1"
                >
                  <span>Review Case File & Evidence</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};
