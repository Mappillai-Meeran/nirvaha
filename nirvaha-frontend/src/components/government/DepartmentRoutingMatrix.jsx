import React, { useState } from 'react';
import { 
  Building2, 
  ArrowRight, 
  CheckCircle2, 
  Clock, 
  AlertTriangle, 
  FileText, 
  Shuffle, 
  ShieldCheck, 
  Users,
  Search
} from 'lucide-react';

/**
 * DepartmentRoutingMatrix
 * 
 * Inter-Department Jurisdiction & Multi-Agency Coordination Matrix.
 * Resolves municipal ping-pong where infrastructure conflicts span multiple agencies
 * (e.g., Water Board pipeline leak damaging a PWD carriageway).
 * Visualizes primary & secondary ownership, joint clearances, and pending sign-offs.
 */
export const DepartmentRoutingMatrix = ({ incidents = [], onSelectIncident }) => {
  const [selectedDeptFilter, setSelectedDeptFilter] = useState('ALL');
  const [selectedJointOnly, setSelectedJointOnly] = useState(false);

  // Departments recognized under Puducherry Municipal Administration
  const departments = [
    { code: 'PWD', name: 'Public Works Department (Roads & Stormwater)', lead: 'Executive Engineer (PWD)' },
    { code: 'LAD', name: 'Local Administration Dept (Municipality)', lead: 'Municipal Commissioner' },
    { code: 'PED', name: 'Puducherry Electricity Department', lead: 'Divisional Engineer (Electrical)' },
    { code: 'PHE', name: 'Water Supply & Sewerage Board', lead: 'Superintending Engineer (PHED)' },
  ];

  // Map category to authoritative departments with joint-clearance rules
  const getDepartmentRouting = (incident) => {
    switch (incident.category) {
      case 'DRAINAGE':
        return {
          primaryDept: 'Local Administration Dept (Municipality)',
          secondaryDept: 'Public Works Department (PWD)',
          jointRequired: true,
          clearanceStatus: incident.status === 'RESOLVED' ? 'CLEARED' : 'PENDING_PWD_CULVERT_CLEARANCE',
          jurisdictionRationale: 'Stormwater canal trunk is maintained by Municipality, but bridge culvert crossing falls under PWD jurisdiction.'
        };
      case 'POTHOLE':
        return {
          primaryDept: 'Public Works Department (Roads)',
          secondaryDept: 'Water Supply & Sewerage Board',
          jointRequired: true,
          clearanceStatus: incident.status === 'RESOLVED' ? 'CLEARED' : 'UNDER_JOINT_PIPE_INSPECTION',
          jurisdictionRationale: 'Carriageway asphalt damage initiated by underground water pipe pressure fissure. Joint road-cut restoration required.'
        };
      case 'STREETLIGHT':
        return {
          primaryDept: 'Puducherry Electricity Department',
          secondaryDept: 'Local Administration Dept',
          jointRequired: false,
          clearanceStatus: 'DIRECT_PED_ASSIGNMENT',
          jurisdictionRationale: 'Sole jurisdiction of Electrical Division. No civil road excavation required.'
        };
      case 'GARBAGE':
        return {
          primaryDept: 'Local Administration Dept (Sanitation)',
          secondaryDept: 'None',
          jointRequired: false,
          clearanceStatus: 'DIRECT_MUNICIPAL_ASSIGNMENT',
          jurisdictionRationale: 'Direct solid waste concessionaire management.'
        };
      default:
        return {
          primaryDept: 'Local Administration Dept',
          secondaryDept: 'None',
          jointRequired: false,
          clearanceStatus: 'ROUTINE_ROUTING',
          jurisdictionRationale: 'Standard single-department municipal ticket.'
        };
    }
  };

  // Enhance incidents with multi-department routing data
  const routedIncidents = incidents.map((inc) => ({
    ...inc,
    routing: getDepartmentRouting(inc)
  }));

  const filteredIncidents = routedIncidents.filter((inc) => {
    if (selectedJointOnly && !inc.routing.jointRequired) return false;
    if (selectedDeptFilter !== 'ALL') {
      const matchPrimary = inc.routing.primaryDept.toLowerCase().includes(selectedDeptFilter.toLowerCase());
      const matchSecondary = inc.routing.secondaryDept.toLowerCase().includes(selectedDeptFilter.toLowerCase());
      if (!matchPrimary && !matchSecondary) return false;
    }
    return true;
  });

  return (
    <div className="bg-white rounded-xl border border-[#d8ddda] p-6 shadow-sm mb-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-[#e5e8e6]">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#174f4a] text-white">
              <Building2 size={13} />
              INTER-AGENCY COORDINATION
            </span>
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200">
              Cross-Department Accountability Matrix
            </span>
          </div>
          <h3 className="text-xl font-bold text-[#1e2b2d] mt-1.5">
            Inter-Department Jurisdiction & Joint Clearance Routing
          </h3>
          <p className="text-xs text-[#596765] mt-0.5">
            Prevents municipal grievance ping-pong by binding primary responsibility and tracking required cross-department sign-offs.
          </p>
        </div>

        {/* Filter Controls */}
        <div className="flex flex-wrap items-center gap-2 self-start sm:self-center">
          <button
            type="button"
            onClick={() => setSelectedJointOnly(!selectedJointOnly)}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold border transition ${
              selectedJointOnly 
                ? 'bg-amber-600 text-white border-amber-600 shadow-xs' 
                : 'bg-white text-[#596765] border-[#d8ddda] hover:bg-[#f0f2ee]'
            }`}
          >
            {selectedJointOnly ? '✓ Joint Clearance Cases Only' : 'Filter Joint Clearance Cases'}
          </button>
        </div>
      </div>

      {/* Department Tabs */}
      <div className="flex flex-wrap items-center gap-2 my-4">
        <span className="text-xs font-bold text-[#596765] uppercase tracking-wider mr-1">Department:</span>
        <button
          type="button"
          onClick={() => setSelectedDeptFilter('ALL')}
          className={`px-3 py-1 rounded-lg text-xs font-bold border transition ${
            selectedDeptFilter === 'ALL'
              ? 'bg-[#174f4a] text-white border-[#174f4a]'
              : 'bg-white text-[#394644] border-[#d8ddda] hover:bg-[#f0f2ee]'
          }`}
        >
          All Departments ({incidents.length})
        </button>

        {departments.map((dept) => {
          const count = routedIncidents.filter(i => 
            i.routing.primaryDept.includes(dept.code) || i.routing.primaryDept.includes(dept.name.split(' ')[0])
          ).length;

          return (
            <button
              key={dept.code}
              type="button"
              onClick={() => setSelectedDeptFilter(dept.code)}
              className={`px-3 py-1 rounded-lg text-xs font-bold border transition ${
                selectedDeptFilter === dept.code
                  ? 'bg-[#174f4a] text-white border-[#174f4a]'
                  : 'bg-white text-[#394644] border-[#d8ddda] hover:bg-[#f0f2ee]'
              }`}
            >
              {dept.code} ({count})
            </button>
          );
        })}
      </div>

      {/* Incident Routing Matrix Table */}
      <div className="border border-[#e8ebe9] rounded-xl overflow-hidden bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#f7f9f8] text-[#596765] uppercase font-bold border-b border-[#e8ebe9]">
              <tr>
                <th className="py-3 px-4">Case & Title</th>
                <th className="py-3 px-4">Primary Accountable Dept</th>
                <th className="py-3 px-4">Secondary / Co-Signatory</th>
                <th className="py-3 px-4">Joint Clearance Status</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e8ebe9]">
              {filteredIncidents.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-xs text-[#66716e]">
                    No cases match the selected department filter.
                  </td>
                </tr>
              ) : (
                filteredIncidents.map((inc) => (
                  <tr key={inc.id} className="hover:bg-[#fafafa] transition">
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-[#1e2b2d]">{inc.title}</div>
                      <div className="text-[11px] text-[#66716e] mt-0.5">
                        Case #{inc.id} • {inc.category} • {inc.address || 'Puducherry'}
                      </div>
                    </td>

                    <td className="py-3.5 px-4 font-semibold text-[#1e2b2d]">
                      <div className="flex items-center gap-1.5">
                        <Building2 size={13} className="text-[#174f4a]" />
                        <span>{inc.routing.primaryDept}</span>
                      </div>
                      <div className="text-[10px] text-[#66716e] mt-0.5">Primary Execution Budget</div>
                    </td>

                    <td className="py-3.5 px-4 text-[#394644]">
                      {inc.routing.secondaryDept !== 'None' ? (
                        <div>
                          <div className="font-semibold text-[#1e2b2d]">{inc.routing.secondaryDept}</div>
                          <div className="text-[10px] text-amber-700 font-medium">Inter-Agency Co-Signatory</div>
                        </div>
                      ) : (
                        <span className="text-[#66716e] italic">Direct Ownership (Sole Dept)</span>
                      )}
                    </td>

                    <td className="py-3.5 px-4">
                      {inc.routing.jointRequired ? (
                        <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                          inc.routing.clearanceStatus === 'CLEARED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-amber-100 text-amber-800 border border-amber-300'
                        }`}>
                          {inc.routing.clearanceStatus === 'CLEARED' ? <CheckCircle2 size={12} /> : <AlertTriangle size={12} />}
                          <span>{inc.routing.clearanceStatus.replace(/_/g, ' ')}</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-slate-100 text-slate-700">
                          <CheckCircle2 size={12} />
                          <span>Direct Clearance</span>
                        </span>
                      )}
                      <p className="text-[10px] text-[#66716e] mt-1 max-w-xs line-clamp-1" title={inc.routing.jurisdictionRationale}>
                        {inc.routing.jurisdictionRationale}
                      </p>
                    </td>

                    <td className="py-3.5 px-4 text-right">
                      <button
                        type="button"
                        onClick={() => onSelectIncident && onSelectIncident(inc)}
                        className="px-3 py-1 rounded bg-[#f0f4f3] hover:bg-[#174f4a] text-[#174f4a] hover:text-white font-bold text-xs transition"
                      >
                        Inspect Dossier
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
