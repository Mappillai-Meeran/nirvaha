import React from 'react';
import { Tag, AlertTriangle } from 'lucide-react';

/**
 * PredominantRiskFactors
 * 
 * Inspired by Sentinella's "Fatores de risco predominantes".
 * Displays ranked operational infrastructure risk factors with frequency pills.
 * Enables officers to identify root systemic causes across municipal complaints.
 */
export const PredominantRiskFactors = ({ incidents = [], onSelectCategory }) => {
  const riskFactors = [
    { count: 8, label: 'Stormwater Grate Clogging', category: 'DRAINAGE', severity: 'HIGH' },
    { count: 6, label: 'Heavy Wheel Bitumen Cavity', category: 'POTHOLE', severity: 'HIGH' },
    { count: 5, label: 'Coastal Saline Cable Deterioration', category: 'STREETLIGHT', severity: 'MODERATE' },
    { count: 4, label: 'Canal Tidal Surcharge Risk', category: 'DRAINAGE', severity: 'CRITICAL' },
    { count: 3, label: 'Excavation Backfill Delay', category: 'POTHOLE', severity: 'MODERATE' },
    { count: 2, label: 'Pipeline Joint Fissure', category: 'WATER_SUPPLY', severity: 'MODERATE' },
  ];

  return (
    <div className="bg-white rounded-xl border border-[#E2E8E5] p-3.5 shadow-sm space-y-3">
      <div className="flex items-center justify-between border-b border-[#EAEFEA] pb-2">
        <div className="flex items-center gap-1.5">
          <Tag size={14} className="text-[#B94D36]" />
          <h4 className="text-xs font-bold uppercase tracking-wider text-[#18201F]">
            Predominant Risk Factors
          </h4>
        </div>
        <span className="text-[10px] font-mono text-[#66716E]">
          Frequency Ranked
        </span>
      </div>

      <div className="space-y-1.5">
        {riskFactors.map((rf, idx) => {
          const pillBorder = 
            rf.severity === 'CRITICAL' ? 'border-[#FCA5A5] bg-[#FEF2F2] text-[#991B1B]' :
            rf.severity === 'HIGH' ? 'border-[#FDE68A] bg-[#FFFBEB] text-[#92400E]' :
            'border-[#E2E8F0] bg-[#F8FAFC] text-[#334155]';

          return (
            <div
              key={idx}
              onClick={() => onSelectCategory && onSelectCategory(rf.category)}
              className={`p-2 rounded-lg border flex items-center justify-between gap-2 cursor-pointer transition hover:opacity-90 ${pillBorder}`}
            >
              <span className="text-xs font-semibold truncate">
                {rf.label}
              </span>
              <span className="font-mono text-xs font-bold px-1.5 py-0.2 rounded bg-white shadow-xs shrink-0">
                {rf.count}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};
