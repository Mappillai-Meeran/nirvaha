import React, { useState } from 'react';
import { 
  RotateCcw, 
  Sliders, 
  Info, 
  ArrowRight
} from 'lucide-react';

/**
 * MonsoonStressSimulator
 * 
 * Interactive Predictive Rainfall Stress-Test Engine for Municipal Officers.
 * Allows simulating +25mm (Heavy Rain) or +50mm (Extreme Cloudburst) scenarios.
 * Dynamically updates ward risk rankings and priority levels using documented formulas.
 * Strictly labels all outputs as SIMULATED and includes a 1-click Reset to Baseline action.
 */
export const MonsoonStressSimulator = ({ 
  wards = [], 
  incidents = [], 
  onSimulateChange 
}) => {
  const [simulatedMm, setSimulatedMm] = useState(0);

  // Apply predictive stress model
  // Baseline risk + (simulatedMm / 50) * CategoryRelevance * 25 points
  const computeSimulatedWards = (mm) => {
    if (mm === 0) return wards;

    return wards.map((w) => {
      const baseRisk = w.riskScore100 != null ? w.riskScore100 : (w.riskScore ? Math.round(w.riskScore * 100) : 50);
      // Drainage-heavy wards (like Ward 05) suffer higher monsoon vulnerability
      const isDrainage = w.topRiskCategory === 'DRAINAGE';
      const surgePoints = isDrainage ? Math.round((mm / 50) * 22) : Math.round((mm / 50) * 12);
      const newRisk = Math.min(100, baseRisk + surgePoints);
      const newLevel = newRisk >= 80 ? 'CRITICAL' : newRisk >= 60 ? 'HIGH' : newRisk >= 40 ? 'MODERATE' : 'LOW';

      return {
        ...w,
        riskScore100: newRisk,
        riskLevel: newLevel,
        isSimulated: true,
        baselineRiskScore: baseRisk,
        riskDelta: surgePoints
      };
    });
  };

  const computeSimulatedIncidents = (mm) => {
    if (mm === 0) return incidents;

    return incidents.map((inc) => {
      if (inc.category === 'DRAINAGE') {
        const surge = mm >= 50 ? 'CRITICAL' : mm >= 25 ? 'HIGH' : inc.severity;
        return {
          ...inc,
          severity: surge,
          simulatedUrgency: mm >= 50 ? 95 : 82,
          isSimulated: true
        };
      }
      return inc;
    });
  };

  const handleSliderChange = (newMm) => {
    setSimulatedMm(newMm);
    const simWards = computeSimulatedWards(newMm);
    const simIncidents = computeSimulatedIncidents(newMm);
    if (onSimulateChange) {
      onSimulateChange(newMm, simWards, simIncidents);
    }
  };

  const handleReset = () => {
    setSimulatedMm(0);
    if (onSimulateChange) {
      onSimulateChange(0, wards, incidents);
    }
  };

  const isSimulated = simulatedMm > 0;
  const simulatedWards = computeSimulatedWards(simulatedMm);

  return (
    <div className={`rounded-xl border p-5 transition ${
      isSimulated 
        ? 'bg-amber-50/70 border-amber-300 shadow-sm ring-1 ring-amber-300' 
        : 'bg-white border-[#d8ddda] shadow-sm'
    }`}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#e5e8e6]">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#174f4a]/10 text-[#174f4a]">
              <Sliders size={13} />
              PREDICTIVE CONTINGENCY DRILL
            </span>

            {isSimulated ? (
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-600 text-white animate-pulse">
                ● SIMULATED SCENARIO (+{simulatedMm}mm)
              </span>
            ) : (
              <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200">
                Baseline (Actual IMD Observations)
              </span>
            )}
          </div>
          <h3 className="text-base font-bold text-[#1e2b2d] mt-1">
            Monsoon Rainfall Stress-Test Simulator
          </h3>
          <p className="text-xs text-[#596765]">
            Simulate hypothetical localized cloudbursts to test ward drainage capacity before weather events occur.
          </p>
        </div>

        {isSimulated && (
          <button
            type="button"
            onClick={handleReset}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold bg-white text-[#174f4a] border border-[#cfdad7] hover:bg-[#f0f4f3] transition shadow-xs self-start sm:self-center"
          >
            <RotateCcw size={13} />
            <span>Reset to Baseline (0 mm)</span>
          </button>
        )}
      </div>

      {/* Simulator Control Options */}
      <div className="mt-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-2">
          <span className="text-xs font-bold text-[#1e2b2d]">Select Precipitation Scenario:</span>
          <span className="font-mono text-xs font-bold text-[#174f4a]">
            {simulatedMm === 0 ? 'Baseline (0 mm Additional Rain)' : `+${simulatedMm} mm Additional Rainfall`}
          </span>
        </div>

        <div className="grid grid-cols-3 gap-2">
          <button
            type="button"
            onClick={() => handleSliderChange(0)}
            className={`py-2 px-3 rounded-lg text-xs font-bold border transition text-center ${
              simulatedMm === 0 
                ? 'bg-[#174f4a] text-white border-[#174f4a] shadow-xs' 
                : 'bg-white text-[#596765] border-[#d8ddda] hover:bg-[#f0f2ee]'
            }`}
          >
            <div>Baseline</div>
            <div className="text-[10px] font-normal opacity-90">0 mm (Current Live)</div>
          </button>

          <button
            type="button"
            onClick={() => handleSliderChange(25)}
            className={`py-2 px-3 rounded-lg text-xs font-bold border transition text-center ${
              simulatedMm === 25 
                ? 'bg-amber-600 text-white border-amber-600 shadow-xs' 
                : 'bg-white text-[#596765] border-[#d8ddda] hover:bg-[#f0f2ee]'
            }`}
          >
            <div>Heavy Rain (+25 mm)</div>
            <div className="text-[10px] font-normal opacity-90">Localized Surcharge</div>
          </button>

          <button
            type="button"
            onClick={() => handleSliderChange(50)}
            className={`py-2 px-3 rounded-lg text-xs font-bold border transition text-center ${
              simulatedMm === 50 
                ? 'bg-rose-600 text-white border-rose-600 shadow-xs' 
                : 'bg-white text-[#596765] border-[#d8ddda] hover:bg-[#f0f2ee]'
            }`}
          >
            <div>Cloudburst (+50 mm)</div>
            <div className="text-[10px] font-normal opacity-90">Extreme Flood Contingency</div>
          </button>
        </div>
      </div>

      {/* Baseline vs Simulated Comparison Table */}
      {isSimulated && (
        <div className="mt-4 pt-3 border-t border-amber-200">
          <div className="text-xs font-bold text-amber-900 mb-2 flex items-center justify-between">
            <span>Predicted Ward Risk Escalation under +{simulatedMm}mm Rain:</span>
            <span className="text-[10px] uppercase font-mono tracking-wider">Hypothetical Simulation</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
            {simulatedWards.map((w) => (
              <div key={w.wardId} className="p-2.5 rounded-lg bg-white border border-amber-200 text-xs shadow-xs">
                <div className="flex items-center justify-between font-bold text-[#1e2b2d]">
                  <span>{w.wardName}</span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    w.riskLevel === 'CRITICAL' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800'
                  }`}>
                    {w.riskLevel}
                  </span>
                </div>
                <div className="flex items-center justify-between mt-2 text-[11px] text-[#596765]">
                  <span>Baseline: {w.baselineRiskScore}/100</span>
                  <ArrowRight size={12} className="text-amber-700" />
                  <strong className="text-rose-700 font-bold">Simulated: {w.riskScore100}/100 (+{w.riskDelta})</strong>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-3 p-2 bg-amber-100/60 rounded text-[11px] text-amber-900 flex items-start gap-1.5">
            <Info size={13} className="shrink-0 mt-0.5 text-amber-700" />
            <span>
              <strong>Integrity Guardrail:</strong> These results are purely analytical projections for contingency planning. 
              Hypothetical scores are never persisted to the official database or used to close tickets automatically.
            </span>
          </div>
        </div>
      )}
    </div>
  );
};
