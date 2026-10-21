import React from 'react';
import { Activity } from 'lucide-react';

/**
 * InfrastructureSpiderRadar
 * 
 * Inspired by the Power BI World Championship dashboard (Image 2).
 * 5-axis SVG Radar / Spider polygon chart visualizing citywide infrastructure
 * vulnerability across 5 core municipal domains:
 * 1. Transit & Roads (POTHOLE)
 * 2. Stormwater (DRAINAGE)
 * 3. Public Lighting (STREETLIGHT)
 * 4. Sanitation (GARBAGE)
 * 5. Water Supply (WATER_SUPPLY)
 */
export const InfrastructureSpiderRadar = ({ incidents = [] }) => {
  // Compute normalized load for each of the 5 domains (0 to 100)
  const getDomainScore = (cat, defaultScore) => {
    const total = incidents.filter(i => i.category === cat).length;
    if (total === 0) return defaultScore;
    return Math.min(100, Math.max(20, total * 18));
  };

  const axes = [
    { label: 'Drainage', score: getDomainScore('DRAINAGE', 85) },
    { label: 'Roads', score: getDomainScore('POTHOLE', 65) },
    { label: 'Lighting', score: getDomainScore('STREETLIGHT', 45) },
    { label: 'Sanitation', score: getDomainScore('GARBAGE', 55) },
    { label: 'Water', score: getDomainScore('WATER_SUPPLY', 30) },
  ];

  const totalAxes = axes.length;
  const center = 75;
  const maxRadius = 55;

  // Compute (x, y) coordinates for angle and radius
  const getCoordinates = (index, valuePercent) => {
    const angle = (Math.PI * 2 * index) / totalAxes - Math.PI / 2; // start from top
    const r = (valuePercent / 100) * maxRadius;
    const x = center + r * Math.cos(angle);
    const y = center + r * Math.sin(angle);
    return { x, y };
  };

  // Generate web rings (25%, 50%, 75%, 100%)
  const webRings = [25, 50, 75, 100];

  // Generate polygon points for data
  const dataPoints = axes.map((a, i) => getCoordinates(i, a.score));
  const polygonPointsStr = dataPoints.map(p => `${p.x},${p.y}`).join(' ');

  return (
    <div className="bg-white rounded-xl border border-[#E2E8E5] p-3.5 shadow-sm space-y-2">
      <div className="flex items-center justify-between border-b border-[#EAEFEA] pb-2">
        <div className="flex items-center gap-1.5">
          <Activity size={14} className="text-[#2563EB]" />
          <h4 className="text-xs font-bold uppercase tracking-wider text-[#18201F]">
            5-Axis Domain Vulnerability
          </h4>
        </div>
        <span className="text-[10px] font-mono text-[#66716E]">
          Spider Radar
        </span>
      </div>

      <div className="relative w-full h-44 flex items-center justify-center">
        <svg viewBox="0 0 150 150" className="w-full h-full max-w-[170px]">
          {/* Concentric Web Rings */}
          {webRings.map((ring) => {
            const ringPoints = Array.from({ length: totalAxes }).map((_, i) => {
              const p = getCoordinates(i, ring);
              return `${p.x},${p.y}`;
            }).join(' ');

            return (
              <polygon
                key={ring}
                points={ringPoints}
                fill="none"
                stroke="#E2E8F0"
                strokeWidth="0.8"
                strokeDasharray={ring === 100 ? undefined : '2,2'}
              />
            );
          })}

          {/* Radial Spokes from Center */}
          {Array.from({ length: totalAxes }).map((_, i) => {
            const end = getCoordinates(i, 100);
            return (
              <line
                key={i}
                x1={center}
                y1={center}
                x2={end.x}
                y2={end.y}
                stroke="#CBD5E1"
                strokeWidth="0.8"
              />
            );
          })}

          {/* Data Filled Polygon */}
          <polygon
            points={polygonPointsStr}
            fill="#2563EB"
            fillOpacity="0.25"
            stroke="#2563EB"
            strokeWidth="1.8"
            className="transition-all duration-700"
          />

          {/* Data Vertex Dots */}
          {dataPoints.map((p, i) => (
            <circle
              key={i}
              cx={p.x}
              cy={p.y}
              r="2.5"
              fill="#2563EB"
              stroke="#FFFFFF"
              strokeWidth="1"
            />
          ))}

          {/* Axis Labels */}
          {axes.map((a, i) => {
            const labelCoord = getCoordinates(i, 122);
            return (
              <text
                key={i}
                x={labelCoord.x}
                y={labelCoord.y + 2}
                textAnchor="middle"
                fontSize="7.5"
                fontWeight="700"
                fill="#475569"
                fontFamily="sans-serif"
              >
                {a.label} ({a.score}%)
              </text>
            );
          })}
        </svg>
      </div>

      <div className="text-[10px] text-[#66716E] text-center pt-1 border-t border-[#F1F5F3]">
        Multi-dimensional load across 5 gazette municipal domains
      </div>
    </div>
  );
};
