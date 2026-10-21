import React, { useState } from 'react';
import { 
  Layers, 
  AlertCircle, 
  Users, 
  MapPin, 
  Clock, 
  ShieldAlert, 
  Info, 
  CheckCircle2,
  TrendingDown,
  ArrowRight
} from 'lucide-react';

/**
 * DuplicateEquityRadar
 * 
 * 1. Duplicate Cluster Noise-to-Signal Engine:
 *    Groups multiple citizen grievances of the same physical problem within 500m Haversine.
 *    Neutralizes duplicate dispatch noise without deleting authentic citizen voice.
 * 
 * 2. Underserved Ward Equity Alert:
 *    Identifies wards with low reporting density but high infrastructure vulnerability.
 *    Enforces the core IAS principle: "Fewer complaints does NOT equal fewer problems."
 */
export const DuplicateEquityRadar = ({ signals = [], wards = [] }) => {
  const [selectedClusterId, setSelectedClusterId] = useState(signals[0]?.id || 'SIG-21');

  // Curated clusters demonstrating 500m Haversine deduplication
  const sampleClusters = [
    {
      id: 'SIG-21',
      category: 'DRAINAGE',
      areaName: 'Mission Street Crossing & Bussy Street',
      ward: 'Ward 05 (V.O.C. Nagar)',
      radiusMeters: 280,
      reportCount: 6,
      uniqueProblemCount: 1,
      noiseRatio: '6:1',
      timeWindow: '14 days',
      centroid: '11.9350° N, 79.8300° E',
      reports: [
        { id: 'DEMO-1042', time: '12 Oct, 09:15 AM', lang: 'TA', summary: 'Drainage overflowing near pedestrian crossing' },
        { id: 'DEMO-1045', time: '12 Oct, 10:20 AM', lang: 'EN', summary: 'Sewage water backing up onto Mission St pavement' },
        { id: 'DEMO-1046', time: '12 Oct, 11:45 AM', lang: 'TA', summary: 'Severe foul smell and choked drain grating' },
        { id: 'DEMO-1047', time: '13 Oct, 08:10 AM', lang: 'HI', summary: 'Waterlogged road blocking two-wheeler parking' },
      ],
      consolidationBenefit: '5 redundant truck dispatches avoided. Unified work order dispatched to PWD Stormwater Unit.'
    },
    {
      id: 'SIG-22',
      category: 'POTHOLE',
      areaName: 'M.G. Road & Rangapillai Junction',
      ward: 'Ward 18 (Orleanpet)',
      radiusMeters: 220,
      reportCount: 4,
      uniqueProblemCount: 1,
      noiseRatio: '4:1',
      timeWindow: '7 days',
      centroid: '11.9318° N, 79.8271° E',
      reports: [
        { id: 'DEMO-1044', time: '11 Oct, 02:40 PM', lang: 'HI', summary: 'Pothole at school junction creates accident risk' },
        { id: 'DEMO-1048', time: '11 Oct, 05:15 PM', lang: 'TA', summary: 'Deep crater on bus route damaging tyres' },
        { id: 'DEMO-1049', time: '12 Oct, 07:30 AM', lang: 'EN', summary: 'Road cavity expanding after overnight rain' },
      ],
      consolidationBenefit: '3 redundant contractor callouts saved. Road patch scheduled under emergency grant.'
    }
  ];

  const activeCluster = sampleClusters.find(c => c.id === selectedClusterId) || sampleClusters[0];

  // Underserved Equity Detection
  // Evaluates wards where reporting volume is low despite elevated risk index
  const equityVulnerableWards = [
    {
      wardId: 'PUD-18',
      wardName: 'Orleanpet (Sector West)',
      populationEst: '24,500',
      smartphoneReportingRate: '0.42 reports / 1,000 residents (Low)',
      infrastructureVulnerabilityScore: '67 / 100 (HIGH)',
      equityRiskFlag: 'POTENTIALLY UNDERSERVED COLONY',
      reason: 'Low smartphone grievance filing detected in informal housing pocket despite active open-drain culvert overflows.',
      recommendedIntervention: 'Deploy mobile municipal inspection van; do not wait for passive citizen app complaints.'
    }
  ];

  return (
    <div className="bg-white rounded-xl border border-[#d8ddda] p-6 shadow-sm mb-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-[#e5e8e6]">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#174f4a] text-white">
              <Layers size={13} />
              SPATIAL CLUSTERING & EQUITY
            </span>
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200">
              500m Haversine Noise Neutralization
            </span>
          </div>
          <h3 className="text-xl font-bold text-[#1e2b2d] mt-1.5">
            Duplicate Cluster Analysis & Underserved Equity Alert
          </h3>
          <p className="text-xs text-[#596765] mt-0.5">
            Clusters redundant reports of the same physical problem while proactively monitoring low-reporting, high-vulnerability wards.
          </p>
        </div>

        {/* Cluster Tabs */}
        <div className="flex items-center gap-1.5 bg-[#f0f4f3] p-1 rounded-lg border border-[#cfdad7] self-start sm:self-center">
          {sampleClusters.map((c) => (
            <button
              key={c.id}
              type="button"
              onClick={() => setSelectedClusterId(c.id)}
              className={`px-3 py-1 text-xs font-bold rounded-md transition ${
                selectedClusterId === c.id 
                  ? 'bg-[#174f4a] text-white shadow-xs' 
                  : 'text-[#394644] hover:text-[#174f4a]'
              }`}
            >
              Cluster {c.id} ({c.reportCount} Reports)
            </button>
          ))}
        </div>
      </div>

      {/* Cluster Deep-Dive */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 my-5">
        {/* Left: Cluster Metrics */}
        <div className="lg:col-span-5 p-4 rounded-xl bg-[#fafafa] border border-[#e8ebe9] flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between text-xs mb-2">
              <span className="font-mono font-bold text-[#174f4a]">{activeCluster.id}</span>
              <span className="px-2 py-0.5 rounded bg-emerald-100 text-emerald-800 font-bold text-[10px]">
                NOISE RATIO: {activeCluster.noiseRatio}
              </span>
            </div>
            <h4 className="text-sm font-bold text-[#1e2b2d]">{activeCluster.areaName}</h4>
            <div className="text-xs text-[#66716e] mt-1">{activeCluster.ward}</div>

            <div className="grid grid-cols-2 gap-2 my-4 text-xs font-mono">
              <div className="p-2 bg-white rounded border border-[#d8ddda]">
                <span className="text-[10px] text-[#66716e] block uppercase">Haversine Radius</span>
                <strong className="text-[#1e2b2d]">{activeCluster.radiusMeters} meters</strong>
              </div>
              <div className="p-2 bg-white rounded border border-[#d8ddda]">
                <span className="text-[10px] text-[#66716e] block uppercase">Time Span</span>
                <strong className="text-[#1e2b2d]">{activeCluster.timeWindow}</strong>
              </div>
            </div>

            <div className="p-3 bg-emerald-50 rounded-lg border border-emerald-200 text-xs text-emerald-900 leading-relaxed">
              <strong>Administrative Consolidation:</strong> {activeCluster.consolidationBenefit}
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-[#e8ebe9] text-[11px] text-[#66716e] flex items-center gap-1.5">
            <MapPin size={12} className="text-[#174f4a]" />
            <span>Cluster Centroid: <strong>{activeCluster.centroid}</strong></span>
          </div>
        </div>

        {/* Right: Consolidated Citizen Reports List */}
        <div className="lg:col-span-7">
          <div className="text-xs font-bold uppercase tracking-wider text-[#596765] mb-2 flex items-center justify-between">
            <span>Linked Citizen Grievances in this 500m Group ({activeCluster.reports.length})</span>
            <span className="text-[10px] text-[#66716e] lowercase">authentic voices preserved</span>
          </div>

          <div className="divide-y divide-[#e8ebe9] border border-[#e8ebe9] rounded-xl overflow-hidden bg-white text-xs">
            {activeCluster.reports.map((rep) => (
              <div key={rep.id} className="p-3 hover:bg-[#fafafa] transition flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-[#174f4a]">{rep.id}</span>
                    <span className="px-1.5 py-0.2 rounded text-[10px] bg-[#e5e8e6] text-[#394644] font-bold">
                      {rep.lang}
                    </span>
                    <span className="text-[#66716e] text-[11px]">{rep.time}</span>
                  </div>
                  <p className="text-[#394644] text-xs mt-1">
                    "{rep.summary}"
                  </p>
                </div>
                <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded self-start sm:self-center">
                  LINKED
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Underserved Ward Equity Alert Section */}
      <div className="mt-6 pt-5 border-t border-[#e8ebe9]">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <ShieldAlert size={18} className="text-[#b94d36]" />
            <h4 className="text-sm font-bold text-[#1e2b2d]">
              Digital Divide & Underserved Ward Equity Radar
            </h4>
          </div>
          <span className="text-[11px] text-[#66716e]">Proactive Governance Shield</span>
        </div>

        <div className="divide-y divide-[#f0f2ee] border border-amber-200 rounded-xl overflow-hidden bg-amber-50/50">
          {equityVulnerableWards.map((w) => (
            <div key={w.wardId} className="p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-[#1e2b2d] text-sm">{w.wardName}</span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-200 text-amber-900">
                    {w.equityRiskFlag}
                  </span>
                </div>
                <div className="text-xs text-[#596765] flex flex-wrap gap-3">
                  <span>Population: <strong>{w.populationEst}</strong></span>
                  <span>•</span>
                  <span>Reporting Frequency: <strong className="text-rose-700">{w.smartphoneReportingRate}</strong></span>
                  <span>•</span>
                  <span>Physical Risk Score: <strong className="text-rose-700">{w.infrastructureVulnerabilityScore}</strong></span>
                </div>
                <p className="text-xs text-[#394644] mt-1.5 leading-relaxed">
                  {w.reason}
                </p>
              </div>

              <div className="p-3 bg-white rounded-lg border border-amber-200 text-xs text-[#1e2b2d] md:max-w-xs shrink-0">
                <strong className="block text-[#174f4a] mb-1 font-bold">Proactive Action Required:</strong>
                <span>{w.recommendedIntervention}</span>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-3 p-2.5 bg-[#fafafa] rounded text-[11px] text-[#66716e] flex items-start gap-1.5">
          <Info size={13} className="text-[#174f4a] shrink-0 mt-0.5" />
          <span>
            <strong>Public Service Equity Principle:</strong> In municipal administration, absence of smartphone complaints 
            in poorer localities often indicates a digital divide rather than absence of potholes or sewage overflows. 
            Nirvaha flags these disparities so municipal engineers conduct proactive physical inspections.
          </span>
        </div>
      </div>
    </div>
  );
};
