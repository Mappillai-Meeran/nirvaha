import React, { useEffect, useState, useMemo } from 'react';
import { incidentService, priorityService, workflowService } from '../../services';
import { Card, Badge, LoadingState, ErrorState } from '../../components/ui';
import { Flame, ArrowRight, UserCheck, Search, X, CheckCircle2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const PriorityQueue = () => {
  const navigate = useNavigate();
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [priorityDetails, setPriorityDetails] = useState(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [severityFilter, setSeverityFilter] = useState('ALL');

  const fetchIncidents = async () => {
    try {
      const data = await incidentService.getAllIncidents();
      const list = data || [];
      setIncidents(list);
      if (list.length > 0) {
        handleSelectIncident(list[0]);
      }
    } catch (err) {
      setError(err.message || 'Failed to load priority queue');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectIncident = async (inc) => {
    setSelectedIncident(inc);
    try {
      const priority = await priorityService.getPriority(inc.id).catch(() => null);
      setPriorityDetails(priority);
    } catch (err) {
      console.warn('Failed to load priority detail:', err);
    }
  };

  useEffect(() => {
    let isMounted = true;
    incidentService.getAllIncidents()
      .then((data) => {
        if (!isMounted) return;
        const list = data || [];
        setIncidents(list);
        if (list.length > 0) {
          handleSelectIncident(list[0]);
        }
      })
      .catch((err) => {
        if (!isMounted) return;
        setError(err.message || 'Failed to load priority queue');
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => { isMounted = false; };
  }, []);

  const filteredIncidents = useMemo(() => {
    return incidents.filter((inc) => {
      if (categoryFilter !== 'ALL' && inc.category !== categoryFilter) return false;
      if (severityFilter !== 'ALL' && inc.severity !== severityFilter) return false;
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesId = inc.id?.toString().toLowerCase().includes(q);
        const matchesTitle = inc.title?.toLowerCase().includes(q);
        const matchesDesc = (inc.originalDescription || inc.description || '').toLowerCase().includes(q);
        if (!matchesId && !matchesTitle && !matchesDesc) return false;
      }
      return true;
    });
  }, [incidents, categoryFilter, severityFilter, searchQuery]);

  if (loading) return <LoadingState message="Evaluating multi-factor priority matrix telemetry..." />;
  if (error) return <ErrorState message={error} onRetry={fetchIncidents} />;

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 border-b border-[#D9DEDB] pb-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-[#18201F] flex items-center space-x-2">
            <Flame className="w-5 h-5 text-[#b94d36]" />
            <span>Municipal Priority Matrix Queue</span>
          </h1>
          <p className="text-xs text-[#66716E] mt-0.5">
            Algorithmic priority ranking combining Base Severity, 500m Corroboration, SLA Deadlines, and IMD Weather Telemetry.
          </p>
        </div>

        {/* Filter Controls */}
        <div className="flex items-center flex-wrap gap-2">
          <div className="relative">
            <Search className="w-3.5 h-3.5 text-[#66716E] absolute left-2.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search case..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8 pr-6 py-1.5 bg-white border border-[#D9DEDB] rounded text-xs text-[#18201F] focus:outline-none focus:border-[#174f4a] w-36 sm:w-44"
            />
            {searchQuery && (
              <button onClick={() => setSearchQuery('')} className="absolute right-2 top-1/2 -translate-y-1/2 text-xs text-[#66716E]">
                <X size={12} />
              </button>
            )}
          </div>

          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="px-2 py-1.5 bg-white border border-[#D9DEDB] rounded text-xs text-[#18201F]"
            aria-label="Filter by Category"
          >
            <option value="ALL">All Categories</option>
            <option value="DRAINAGE">Drainage</option>
            <option value="POTHOLE">Potholes</option>
            <option value="STREETLIGHT">Streetlights</option>
            <option value="GARBAGE">Garbage</option>
            <option value="WATER_SUPPLY">Water Supply</option>
          </select>

          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            className="px-2 py-1.5 bg-white border border-[#D9DEDB] rounded text-xs text-[#18201F]"
            aria-label="Filter by Severity"
          >
            <option value="ALL">All Severities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Left Column: Priority Incident List */}
        <div className="lg:col-span-7 space-y-2.5">
          {incidents.length === 0 ? (
            <div className="border border-dashed border-[#D9DEDB] rounded-lg p-12 text-center space-y-2 bg-white">
              <CheckCircle2 className="w-8 h-8 text-[#174f4a] mx-auto" />
              <h4 className="text-sm font-bold text-[#18201F]">Priority Queue Clear</h4>
              <p className="text-xs text-[#66716E] max-w-md mx-auto">
                No open civic incidents requiring priority triage. All municipal reports are up to date.
              </p>
            </div>
          ) : filteredIncidents.length === 0 ? (
            <div className="text-center py-12 text-xs text-[#66716E] bg-white border border-[#D9DEDB] rounded-lg space-y-2">
              <div>No incidents match the active filter criteria ({incidents.length} total cases in database).</div>
              <button
                onClick={() => {
                  setCategoryFilter('ALL');
                  setSeverityFilter('ALL');
                  setSearchQuery('');
                }}
                className="text-xs font-bold text-[#174f4a] hover:underline"
              >
                Reset filters
              </button>
            </div>
          ) : (
            filteredIncidents.map((inc) => (
              <Card 
                key={inc.id} 
                className={`cursor-pointer transition hover:border-[#174f4a] ${
                  selectedIncident?.id === inc.id ? 'border-[#174f4a] bg-[#174f4a]/5 shadow-xs' : 'border-[#D9DEDB] bg-white'
                }`}
                onClick={() => handleSelectIncident(inc)}
              >
                <div className="flex items-start justify-between">
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-xs font-bold text-[#174f4a]">CASE #{inc.id}</span>
                      <Badge variant={inc.status === 'RESOLVED' || inc.status === 'VERIFIED' ? 'success' : 'primary'}>
                        {inc.status}
                      </Badge>
                      <span className="px-2 py-0.5 rounded bg-[#F5F4EF] text-[#18201F] font-mono text-[10px] font-bold border border-[#D9DEDB]">
                        {inc.category}
                      </span>
                    </div>
                    <h3 className="font-bold text-xs text-[#18201F] leading-snug">{inc.title}</h3>
                    <p className="text-xs text-[#66716E] line-clamp-1">{inc.address} • {inc.originalDescription || inc.description}</p>
                  </div>

                  <div className="text-right">
                    <Badge variant={inc.severity === 'HIGH' || inc.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                      {inc.severity}
                    </Badge>
                  </div>
                </div>
              </Card>
            ))
          )}
        </div>

        {/* Right Column: Priority Details & Action */}
        <div className="lg:col-span-5 space-y-4">
          {selectedIncident ? (
            <Card className="space-y-3.5 border-[#D9DEDB] bg-white shadow-sm">
              <div className="border-b border-[#D9DEDB] pb-3">
                <div className="flex justify-between items-center">
                  <span className="font-mono text-[10px] text-[#66716E] font-bold">CASE DETAILS #{selectedIncident.id}</span>
                  <button
                    onClick={() => navigate(`/incidents/${selectedIncident.id}`)}
                    className="text-xs text-[#174f4a] hover:underline flex items-center space-x-1 font-semibold"
                  >
                    <span>View Full Case File</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
                <h3 className="text-sm font-bold text-[#18201F] mt-1 leading-snug">{selectedIncident.title}</h3>
                <p className="text-xs text-[#66716E] mt-1.5 leading-relaxed">{selectedIncident.address}</p>
                <p className="text-xs text-[#18201F] mt-2 leading-relaxed bg-[#F5F4EF] p-2.5 rounded border border-[#D9DEDB]">
                  {selectedIncident.originalDescription || selectedIncident.description}
                </p>
              </div>

              {priorityDetails ? (
                <div className="space-y-2 bg-[#F5F4EF] p-3 rounded border border-[#D9DEDB] text-xs font-mono">
                  <div className="flex justify-between items-center text-[#18201F]">
                    <span className="text-[#66716E]">Calculated Priority Score:</span>
                    <span className="font-bold text-[#b94d36] text-sm">{priorityDetails.priorityScore} / 100</span>
                  </div>
                  <div className="flex justify-between items-center text-[#18201F]">
                    <span className="text-[#66716E]">Assigned Priority Tier:</span>
                    <Badge variant={priorityDetails.priorityLevel === 'CRITICAL' ? 'critical' : 'warning'}>
                      {priorityDetails.priorityLevel}
                    </Badge>
                  </div>
                  <p className="text-[11px] text-[#66716E] mt-1 leading-relaxed border-t border-[#D9DEDB] pt-2 font-sans">
                    {priorityDetails.explanation || 'Priority calculated from base severity, spatial corroboration clusters, weather context, and SLA targets.'}
                  </p>
                </div>
              ) : (
                <div className="text-xs text-[#66716E] font-mono py-2">Loading priority telemetry breakdown...</div>
              )}

              <div className="pt-2">
                <button
                  onClick={async () => {
                    await workflowService.assignOfficer(selectedIncident.id, 1);
                    fetchIncidents();
                  }}
                  className="w-full py-2 bg-[#174f4a] hover:bg-[#123e3a] text-white font-bold text-xs rounded transition flex items-center justify-center space-x-1.5 shadow-sm"
                >
                  <UserCheck className="w-3.5 h-3.5" />
                  <span>Assign Case to Municipal Field Officer</span>
                </button>
              </div>
            </Card>
          ) : (
            <Card className="text-center py-8 text-xs text-[#66716E] border-[#D9DEDB]">
              Select an incident from the priority queue to inspect breakdown details.
            </Card>
          )}
        </div>
      </div>
    </div>
  );
};
