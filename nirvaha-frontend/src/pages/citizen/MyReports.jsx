import React, { useEffect, useState } from 'react';
import { incidentService } from '../../services';
import { Card, Badge, LoadingState, ErrorState } from '../../components/ui';
import { Clock, ArrowRight, FileText } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useLanguage } from '../../context/LanguageContext';

export const MyReports = () => {
  const navigate = useNavigate();
  const { t, getStatusLabel } = useLanguage();
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [unauthorized, setUnauthorized] = useState(false);

  const fetchReports = () => {
    setLoading(true);
    setError(null);
    incidentService.getMyReports()
      .then((data) => {
        setIncidents(data || []);
        setUnauthorized(false);
      })
      .catch((err) => {
        if (err?.status === 401 || err?.status === 403) {
          setUnauthorized(true);
        } else {
          setError(err.message || 'Failed to load resident reports');
        }
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchReports();
  }, []);

  if (loading) return <LoadingState message={t('reports.loading')} />;
  if (unauthorized) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
        <Card className="text-center py-10 space-y-4 border-[#D9DEDB]">
          <h2 className="text-lg font-bold text-[#18201F]">Authentication Required</h2>
          <p className="text-xs text-[#66716E] max-w-md mx-auto">
            Please log in to your Puducherry resident account to view and track your submitted civic reports.
          </p>
          <div>
            <button
              onClick={() => navigate('/login')}
              className="px-4 py-2 bg-[#163A36] text-white text-xs font-semibold rounded-lg hover:bg-[#163A36]/90 transition"
            >
              Sign In to Your Account
            </button>
          </div>
        </Card>
      </div>
    );
  }
  if (error) return <ErrorState message={error} onRetry={fetchReports} />;

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#18201F]">{t('reports.title')}</h1>
        <p className="text-xs text-[#66716E] mt-0.5">{t('reports.subtitle')}</p>
      </div>

      <div className="space-y-4">
        {incidents.length === 0 ? (
          <Card className="text-center py-12 px-4 space-y-4 border-[#D9DEDB] bg-white">
            <div className="w-12 h-12 bg-[#163A36]/10 text-[#163A36] rounded-full flex items-center justify-center mx-auto">
              <FileText className="w-6 h-6 text-[#163A36]" />
            </div>
            <div>
              <h3 className="font-bold text-base text-[#18201F]">No Civic Reports Submitted Yet</h3>
              <p className="text-xs text-[#66716E] max-w-sm mx-auto mt-1">
                You haven't reported any civic complaints from your resident account yet.
              </p>
            </div>
            <div>
              <button
                onClick={() => navigate('/citizen/report')}
                className="px-4 py-2 bg-[#163A36] hover:bg-[#2F6F68] text-white text-xs font-semibold rounded-lg transition"
              >
                Report a Civic Issue
              </button>
            </div>
          </Card>
        ) : (
          incidents.map((inc) => (
            <Card key={inc.id} className="space-y-3 border-[#D9DEDB] hover:border-[#163A36]/40 transition">
              <div className="flex items-start justify-between">
                <div>
                  <span className="font-mono text-xs text-[#2F6F68] font-bold">REPORT #{inc.id}</span>
                  <h3 className="font-bold text-base text-[#18201F] mt-0.5">{inc.title}</h3>
                  <p className="text-xs text-[#66716E]">{inc.address}</p>
                </div>
                <Badge variant={inc.status === 'RESOLVED' || inc.status === 'VERIFIED' ? 'success' : 'primary'}>
                  {getStatusLabel(inc.status)}
                </Badge>
              </div>

              <p className="text-xs text-[#18201F] bg-[#F5F4EF] p-3 rounded-lg border border-[#D9DEDB]">
                {inc.description}
              </p>

              <div className="flex items-center justify-between text-[11px] text-[#66716E] pt-1 border-t border-[#D9DEDB]">
                <div className="flex items-center space-x-2">
                  <Clock className="w-3.5 h-3.5 text-[#66716E]" />
                  <span>{t('reports.reported')}: {new Date(inc.createdAt).toLocaleString()}</span>
                </div>

                <button
                  onClick={() => navigate(`/incidents/${inc.id}`)}
                  className="text-xs text-[#163A36] font-bold hover:underline flex items-center space-x-1"
                >
                  <span>{t('reports.track')}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};
