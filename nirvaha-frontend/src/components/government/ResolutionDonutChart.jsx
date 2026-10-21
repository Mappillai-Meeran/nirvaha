import React from 'react';
import { ShieldCheck } from 'lucide-react';

/**
 * ResolutionDonutChart
 * 
 * Inspired by Sentinella's "PNCD executado" Donut Chart.
 * Displays municipal resolution verification rate with center inspection metric
 * and segment breakdown (Verified, Active/Pending, Audit Flagged).
 */
export const ResolutionDonutChart = ({ incidents = [], anomalies = [] }) => {
  const verified = incidents.filter(i => i.status === 'RESOLVED' || i.status === 'VERIFIED').length || 23;
  const inProgress = incidents.filter(i => i.status === 'IN_PROGRESS' || i.status === 'REPORTED').length || 13;
  const flagged = anomalies.length || 4;

  const totalCalculated = verified + inProgress + flagged;

  // SVG Donut metrics
  const radius = 38;
  const circumference = 2 * Math.PI * radius;
  
  const verifiedArc = (verified / totalCalculated) * circumference;
  const inProgressArc = (inProgress / totalCalculated) * circumference;
  const flaggedArc = (flagged / totalCalculated) * circumference;

  return (
    <div className="bg-white rounded-xl border border-[#E2E8E5] p-3.5 shadow-sm space-y-3">
      <div className="flex items-center justify-between border-b border-[#EAEFEA] pb-2">
        <div className="flex items-center gap-1.5">
          <ShieldCheck size={14} className="text-[#17483B]" />
          <h4 className="text-xs font-bold uppercase tracking-wider text-[#18201F]">
            Resolution Verification
          </h4>
        </div>
        <span className="text-[10px] font-mono text-[#66716E]">
          Evidence Audited
        </span>
      </div>

      <div className="flex items-center justify-between gap-3 pt-1">
        {/* SVG Donut */}
        <div className="relative w-24 h-24 flex items-center justify-center shrink-0">
          <svg viewBox="0 0 100 100" className="w-full h-full -rotate-90">
            {/* Background Track */}
            <circle
              cx="50"
              cy="50"
              r={radius}
              fill="none"
              stroke="#F1F5F3"
              strokeWidth="12"
            />
            {/* Verified (Emerald) */}
            <circle
              cx="50"
              cy="50"
              r={radius}
              fill="none"
              stroke="#15803D"
              strokeWidth="12"
              strokeDasharray={`${verifiedArc} ${circumference}`}
              strokeDashoffset="0"
              strokeLinecap="round"
            />
            {/* In Progress (Amber) */}
            <circle
              cx="50"
              cy="50"
              r={radius}
              fill="none"
              stroke="#D97706"
              strokeWidth="12"
              strokeDasharray={`${inProgressArc} ${circumference}`}
              strokeDashoffset={`-${verifiedArc}`}
              strokeLinecap="round"
            />
            {/* Flagged (Crimson) */}
            <circle
              cx="50"
              cy="50"
              r={radius}
              fill="none"
              stroke="#DC2626"
              strokeWidth="12"
              strokeDasharray={`${flaggedArc} ${circumference}`}
              strokeDashoffset={`-${verifiedArc + inProgressArc}`}
              strokeLinecap="round"
            />
          </svg>

          {/* Center Metric */}
          <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
            <span className="text-lg font-black text-[#18201F] leading-none">
              {verified}
            </span>
            <span className="text-[8px] font-bold uppercase tracking-wider text-[#66716E] mt-0.5">
              Verified
            </span>
          </div>
        </div>

        {/* Legend List */}
        <div className="space-y-1.5 flex-1 text-xs">
          <div className="flex items-center justify-between text-[#18201F]">
            <span className="flex items-center gap-1.5 text-[11px]">
              <span className="w-2 h-2 rounded-full bg-[#15803D]" />
              Verified Fixed
            </span>
            <strong className="font-mono text-xs">{verified}</strong>
          </div>

          <div className="flex items-center justify-between text-[#18201F]">
            <span className="flex items-center gap-1.5 text-[11px]">
              <span className="w-2 h-2 rounded-full bg-[#D97706]" />
              In Progress
            </span>
            <strong className="font-mono text-xs">{inProgress}</strong>
          </div>

          <div className="flex items-center justify-between text-[#18201F]">
            <span className="flex items-center gap-1.5 text-[11px]">
              <span className="w-2 h-2 rounded-full bg-[#DC2626]" />
              Audit Review
            </span>
            <strong className="font-mono text-xs">{flagged}</strong>
          </div>
        </div>
      </div>
    </div>
  );
};
