import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { 
  CloudRain, 
  Calendar, 
  RefreshCw, 
  AlertTriangle, 
  Info, 
  Database
} from 'lucide-react';
import { provenanceService } from '../../services';

/**
 * HistoricalPrecipitationPanel
 * 
 * Curated historical rainfall telemetry visualization for the Government Command Center.
 * Fetches verified hourly reanalysis data from `/api/provenance/weather/history` (Open-Meteo ERA5 archive).
 * 
 * Strict integrity rules:
 * 1. Clearly labeled as historical atmospheric reanalysis, NEVER live weather or street gauges.
 * 2. Uses backend-calculated cumulative and peak metrics without duplicating logic.
 * 3. Shows honest loading, cached, empty, and unavailable error states.
 */
export const HistoricalPrecipitationPanel = ({
  defaultLat = 11.9156,
  defaultLon = 79.8296,
  onClose
}) => {
  // Preset dates: default to the past 7 days up to yesterday
  const getDefaultDates = () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const sevenDaysAgo = new Date(yesterday);
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);

    const fmt = (d) => d.toISOString().split('T')[0];
    return {
      start: fmt(sevenDaysAgo),
      end: fmt(yesterday),
    };
  };

  const initialDates = useMemo(() => getDefaultDates(), []);
  const [startDate, setStartDate] = useState(initialDates.start);
  const [endDate, setEndDate] = useState(initialDates.end);
  const latitude = defaultLat;
  const longitude = defaultLon;

  const [weatherData, setWeatherData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [hoveredPoint, setHoveredPoint] = useState(null);
  const [dateValidationError, setDateValidationError] = useState(null);

  // Maximum allowed end date is yesterday (reanalysis archive lags real-time by ~1-2 days)
  const maxAllowedDate = useMemo(() => {
    const y = new Date();
    y.setDate(y.getDate() - 1);
    return y.toISOString().split('T')[0];
  }, []);

  const validateDates = (start, end) => {
    if (!start || !end) {
      return 'Please specify both start and end dates.';
    }
    const s = new Date(start);
    const e = new Date(end);
    if (isNaN(s.getTime()) || isNaN(e.getTime())) {
      return 'Invalid date format.';
    }
    if (s > e) {
      return 'Start date must be before or equal to end date.';
    }
    const diffDays = Math.round((e - s) / (1000 * 60 * 60 * 24));
    if (diffDays > 31) {
      return 'Maximum date range is 31 days per historical query to prevent payload degradation.';
    }
    return null;
  };

  const fetchHistoricalData = useCallback(async (start = startDate, end = endDate, lat = latitude, lon = longitude) => {
    const valError = validateDates(start, end);
    if (valError) {
      setDateValidationError(valError);
      return;
    }
    setDateValidationError(null);
    setLoading(true);
    setError(null);

    try {
      const response = await provenanceService.getHistoricalWeather(lat, lon, start, end);
      const data = response?.data || response;
      setWeatherData(data);
      if (data && data.available === false) {
        setError(data.sourceMessage || 'Historical weather archive returned no data for this window.');
      }
    } catch (err) {
      console.error('Failed to fetch historical precipitation:', err);
      setError(err?.response?.data?.message || err.message || 'Upstream historical weather service connection failure.');
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate, latitude, longitude]);

  useEffect(() => {
    fetchHistoricalData(initialDates.start, initialDates.end, latitude, longitude);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Preset handlers
  const handlePresetSelect = (presetKey) => {
    const today = new Date();
    let s = new Date();
    let e = new Date();

    if (presetKey === 'PAST_7_DAYS') {
      e.setDate(today.getDate() - 1);
      s.setDate(e.getDate() - 6);
    } else if (presetKey === 'PAST_14_DAYS') {
      e.setDate(today.getDate() - 1);
      s.setDate(e.getDate() - 13);
    } else if (presetKey === 'CYCLONE_NIVAR_2020') {
      // Historical extreme rainfall event in Puducherry: Nov 24 to Nov 27, 2020
      s = new Date('2020-11-24');
      e = new Date('2020-11-27');
    } else if (presetKey === 'NORTHEAST_MONSOON_2021') {
      // Puducherry severe flood week: Nov 10 to Nov 15, 2021
      s = new Date('2021-11-10');
      e = new Date('2021-11-15');
    }

    const fmt = (d) => d.toISOString().split('T')[0];
    const sStr = fmt(s);
    const eStr = fmt(e);
    setStartDate(sStr);
    setEndDate(eStr);
    fetchHistoricalData(sStr, eStr, latitude, longitude);
  };

  const hourlyPoints = useMemo(() => weatherData?.hourlyData || [], [weatherData]);

  // Chart layout calculations
  const chartHeight = 160;
  const chartPadding = { top: 15, bottom: 25, left: 35, right: 15 };
  const maxRainfall = useMemo(() => {
    if (!hourlyPoints || hourlyPoints.length === 0) return 10;
    const max = Math.max(...hourlyPoints.map(p => p.precipitationMm || 0));
    return max > 0 ? Math.ceil(max * 1.2) : 5;
  }, [hourlyPoints]);

  return (
    <div className="bg-white border border-[#E2E8E5] rounded-xl shadow-xs overflow-hidden flex flex-col">
      {/* 1. Header Bar with Rich Forest Emerald Aesthetics */}
      <div className="bg-[#102D27] text-white p-4 sm:p-5 border-b border-[#17483B] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5 flex-wrap">
            <div className="w-8 h-8 rounded-lg bg-[#17483B] border border-[#236353] flex items-center justify-center text-[#34D399]">
              <CloudRain size={18} />
            </div>
            <h2 className="text-base sm:text-lg font-bold text-white tracking-wide">
              Historical Precipitation Telemetry & Reanalysis
            </h2>
            <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-[#17483B] text-[#34D399] border border-[#236353]">
              OPEN-METEO ERA5 ARCHIVE
            </span>
            {weatherData?.isCached && (
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-950/80 text-emerald-300 border border-emerald-700/50">
                LOCAL CACHE HIT
              </span>
            )}
          </div>
          <p className="text-xs text-[#A3B8B2] max-w-3xl">
            Atmospheric historical reanalysis model covering the Puducherry urban agglomeration. 
            Calibrated against ECMWF ERA5 & ERA5-Land spatial assimilation grids.
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-center">
          <button
            type="button"
            onClick={() => fetchHistoricalData()}
            disabled={loading}
            className="px-3 py-1.5 bg-[#17483B] hover:bg-[#236353] text-[#34D399] text-xs font-semibold rounded-lg border border-[#236353] flex items-center gap-1.5 transition disabled:opacity-50"
            title="Refresh telemetry"
          >
            <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
            <span>Sync Archive</span>
          </button>
          {onClose && (
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 text-[#A3B8B2] hover:text-white rounded-lg hover:bg-[#17483B] transition"
              title="Close panel"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* 2. Mandatory Disclaimer Banner (Civic Integrity Requirement) */}
      <div className="bg-[#FFFBEB] border-b border-[#FDE68A] px-4 py-2.5 text-xs text-[#92400E] flex items-start sm:items-center gap-2.5">
        <Info size={16} className="text-[#D97706] shrink-0 mt-0.5 sm:mt-0" />
        <div className="flex-1 leading-snug">
          <strong className="font-semibold text-[#78350F]">Integrity Notice:</strong>{' '}
          This dataset represents <em>atmospheric reanalysis modeling</em>, not an in-situ street rain gauge or telemetry from stormwater culverts. 
          It does not confirm live street inundation, active waterlogging, or alter statutory grievance SLAs.
        </div>
      </div>

      {/* 3. Date & Bounding Coordinates Controls */}
      <div className="p-4 sm:p-5 border-b border-[#E2E8E5] bg-[#F8FAF9] space-y-3">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          {/* Date Pickers */}
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <label htmlFor="hist-start-date" className="text-xs font-bold text-[#18201F] flex items-center gap-1">
                <Calendar size={13} className="text-[#17483B]" />
                <span>Start:</span>
              </label>
              <input
                id="hist-start-date"
                type="date"
                value={startDate}
                max={maxAllowedDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="text-xs bg-white border border-[#CBD5E1] rounded-md px-2.5 py-1.5 text-[#18201F] font-mono focus:outline-none focus:border-[#17483B]"
              />
            </div>

            <div className="flex items-center gap-2">
              <label htmlFor="hist-end-date" className="text-xs font-bold text-[#18201F] flex items-center gap-1">
                <Calendar size={13} className="text-[#17483B]" />
                <span>End:</span>
              </label>
              <input
                id="hist-end-date"
                type="date"
                value={endDate}
                max={maxAllowedDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="text-xs bg-white border border-[#CBD5E1] rounded-md px-2.5 py-1.5 text-[#18201F] font-mono focus:outline-none focus:border-[#17483B]"
              />
            </div>

            <button
              type="button"
              onClick={() => fetchHistoricalData()}
              disabled={loading}
              className="px-3.5 py-1.5 bg-[#17483B] hover:bg-[#102D27] text-white text-xs font-bold rounded-md shadow-xs transition disabled:opacity-50"
            >
              Query Window
            </button>
          </div>

          {/* Curated Historical Presets */}
          <div className="flex flex-wrap items-center gap-1.5">
            <span className="text-[11px] font-bold text-[#64748B] uppercase tracking-wider mr-1">
              Historical Benchmarks:
            </span>
            <button
              type="button"
              onClick={() => handlePresetSelect('PAST_7_DAYS')}
              className="px-2.5 py-1 bg-white hover:bg-[#E2E8E5] text-[#17483B] border border-[#CBD5E1] text-[11px] font-medium rounded transition"
            >
              Past 7 Days
            </button>
            <button
              type="button"
              onClick={() => handlePresetSelect('PAST_14_DAYS')}
              className="px-2.5 py-1 bg-white hover:bg-[#E2E8E5] text-[#17483B] border border-[#CBD5E1] text-[11px] font-medium rounded transition"
            >
              Past 14 Days
            </button>
            <button
              type="button"
              onClick={() => handlePresetSelect('CYCLONE_NIVAR_2020')}
              className="px-2.5 py-1 bg-[#FEF3C7] hover:bg-[#FDE68A] text-[#92400E] border border-[#F59E0B]/40 text-[11px] font-bold rounded transition"
              title="Cyclone Nivar extreme landfall (Nov 2020)"
            >
              Cyclone Nivar (2020)
            </button>
            <button
              type="button"
              onClick={() => handlePresetSelect('NORTHEAST_MONSOON_2021')}
              className="px-2.5 py-1 bg-[#E0E7FF] hover:bg-[#C7D2FE] text-[#3730A3] border border-[#6366F1]/30 text-[11px] font-bold rounded transition"
              title="Severe Northeast Monsoon inundation period (Nov 2021)"
            >
              Monsoon Peak (2021)
            </button>
          </div>
        </div>

        {/* Validation Error Feedback */}
        {dateValidationError && (
          <div className="text-xs text-red-600 bg-red-50 border border-red-200 rounded px-3 py-1.5 flex items-center gap-2">
            <AlertTriangle size={14} className="shrink-0" />
            <span>{dateValidationError}</span>
          </div>
        )}
      </div>

      {/* 4. Executive Metric Cards Strip */}
      <div className="p-4 sm:p-5 grid grid-cols-2 sm:grid-cols-4 gap-3 bg-white">
        <div className="p-3 rounded-lg border border-[#E2E8E5] bg-[#F8FAF9] flex flex-col justify-between">
          <span className="text-[11px] font-semibold text-[#64748B]">Cumulative Rainfall</span>
          <div className="my-1.5">
            <span className="text-2xl font-black font-mono text-[#18201F]">
              {weatherData?.totalRainfallMm != null 
                ? Number(weatherData.totalRainfallMm).toFixed(1) 
                : '—'}
            </span>
            <span className="text-xs font-bold text-[#64748B] ml-1">mm</span>
          </div>
          <span className="text-[10px] text-[#17483B] font-medium">
            {weatherData?.isCompleteCoverage === false
              ? `Sum of ${weatherData?.availableHoursCount || 0} available hours`
              : 'Complete window sum'}
          </span>
        </div>

        <div className="p-3 rounded-lg border border-[#E2E8E5] bg-[#F8FAF9] flex flex-col justify-between">
          <span className="text-[11px] font-semibold text-[#64748B]">Peak Hourly Intensity</span>
          <div className="my-1.5">
            <span className="text-2xl font-black font-mono text-[#0284C7]">
              {weatherData?.maxHourlyRainfallMm != null 
                ? Number(weatherData.maxHourlyRainfallMm).toFixed(1) 
                : '—'}
            </span>
            <span className="text-xs font-bold text-[#64748B] ml-1">mm/h</span>
          </div>
          <span className="text-[10px] text-[#0284C7] font-medium">Single-hour maximum</span>
        </div>

        <div className="p-3 rounded-lg border border-[#E2E8E5] bg-[#F8FAF9] flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-semibold text-[#64748B]">Observation Timeline</span>
            {weatherData?.isCompleteCoverage === false ? (
              <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-amber-100 text-amber-800 font-bold">
                PARTIAL ({weatherData?.coveragePercentage}%)
              </span>
            ) : weatherData?.isCompleteCoverage === true ? (
              <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-emerald-100 text-emerald-800 font-bold">
                100% COMPLETE
              </span>
            ) : null}
          </div>
          <div className="my-1.5">
            <span className="text-2xl font-black font-mono text-[#18201F]">
              {weatherData?.availableHoursCount ?? hourlyPoints.length}
            </span>
            <span className="text-xs font-bold text-[#64748B] ml-1">
              / {hourlyPoints.length} hrs
            </span>
          </div>
          <span className="text-[10px] text-[#64748B]">
            {weatherData?.missingHoursCount ? `${weatherData.missingHoursCount} unassimilated` : `${weatherData?.startDate || ''} → ${weatherData?.endDate || ''}`}
          </span>
        </div>

        <div className="p-3 rounded-lg border border-[#E2E8E5] bg-[#F8FAF9] flex flex-col justify-between">
          <span className="text-[11px] font-semibold text-[#64748B]">Grid Coordinates</span>
          <div className="my-1.5">
            <span className="text-sm font-bold font-mono text-[#18201F]">
              {latitude.toFixed(4)}° N, {longitude.toFixed(4)}° E
            </span>
          </div>
          <span className="text-[10px] text-[#64748B]">Puducherry Central Grid</span>
        </div>
      </div>

      {/* 5. Hourly Precipitation Histogram / Time-Series Visualization */}
      <div className="p-4 sm:p-5 border-t border-[#E2E8E5] bg-white space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h3 className="text-xs font-bold uppercase tracking-wider text-[#18201F]">
              Hourly Precipitation Reanalysis Series (mm/hour)
            </h3>
            <span className="text-[10px] font-mono text-[#64748B]">
              ECMWF ERA5 0.25° Grid
            </span>
          </div>

          {hoveredPoint && (
            <div className="text-xs font-mono bg-[#102D27] text-white px-2.5 py-1 rounded shadow-xs flex items-center gap-2">
              <span className={hoveredPoint.precipitationMm != null ? 'text-[#34D399] font-bold' : 'text-[#F59E0B] font-bold'}>
                {hoveredPoint.precipitationMm != null ? `${hoveredPoint.precipitationMm.toFixed(1)} mm` : 'Unknown / Unassimilated'}
              </span>
              <span className="text-[#A3B8B2]">
                {hoveredPoint.time?.replace('T', ' ')}
              </span>
            </div>
          )}
        </div>

        {/* State Renderers: Loading / Error / Empty / Chart */}
        {loading ? (
          <div className="h-44 flex flex-col items-center justify-center bg-[#F8FAF9] rounded-lg border border-[#E2E8E5] space-y-2">
            <RefreshCw size={22} className="animate-spin text-[#17483B]" />
            <span className="text-xs text-[#64748B] font-mono">
              Retrieving historical reanalysis records from Open-Meteo archive...
            </span>
          </div>
        ) : error ? (
          <div className="h-44 flex flex-col items-center justify-center bg-red-50/50 rounded-lg border border-red-200 p-4 text-center space-y-1.5">
            <AlertTriangle size={22} className="text-red-600" />
            <span className="text-xs font-bold text-red-900">
              Historical Telemetry Unavailable
            </span>
            <p className="text-xs text-red-700 max-w-md">
              {error}
            </p>
            <button
              type="button"
              onClick={() => fetchHistoricalData()}
              className="mt-2 px-3 py-1 bg-red-800 text-white text-xs font-semibold rounded hover:bg-red-900 transition"
            >
              Retry Connection
            </button>
          </div>
        ) : hourlyPoints.length === 0 ? (
          <div className="h-44 flex flex-col items-center justify-center bg-[#F8FAF9] rounded-lg border border-[#E2E8E5] p-4 text-center space-y-1">
            <CloudRain size={24} className="text-[#94A3B8]" />
            <span className="text-xs font-bold text-[#64748B]">
              No Precipitation Recorded
            </span>
            <p className="text-xs text-[#94A3B8]">
              Atmospheric assimilation indicates 0.0 mm rainfall across all timestamps in the selected date window.
            </p>
          </div>
        ) : (
          <div className="bg-[#F8FAF9] border border-[#E2E8E5] rounded-lg p-3 overflow-x-auto">
            <div className="min-w-[650px]">
              <svg 
                viewBox={`0 0 ${Math.max(650, hourlyPoints.length * 6)} ${chartHeight}`} 
                className="w-full h-44 overflow-visible"
              >
                {/* Horizontal reference grid lines */}
                {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
                  const y = chartPadding.top + (1 - ratio) * (chartHeight - chartPadding.top - chartPadding.bottom);
                  const val = (ratio * maxRainfall).toFixed(1);
                  return (
                    <g key={ratio}>
                      <line 
                        x1={chartPadding.left} 
                        y1={y} 
                        x2={Math.max(650, hourlyPoints.length * 6) - chartPadding.right} 
                        y2={y} 
                        stroke="#E2E8E5" 
                        strokeDasharray="3 3" 
                      />
                      <text 
                        x={chartPadding.left - 6} 
                        y={y + 3} 
                        textAnchor="end" 
                        fontSize="9" 
                        fill="#94A3B8" 
                        fontFamily="monospace"
                      >
                        {val}
                      </text>
                    </g>
                  );
                })}

                {/* Bars */}
                {hourlyPoints.map((point, idx) => {
                  const totalWidth = Math.max(650, hourlyPoints.length * 6) - chartPadding.left - chartPadding.right;
                  const barWidth = Math.max(2, (totalWidth / hourlyPoints.length) - 1.5);
                  const x = chartPadding.left + idx * (totalWidth / hourlyPoints.length);
                  const isMissing = point.precipitationMm === null || point.precipitationMm === undefined;
                  const rain = isMissing ? 0 : point.precipitationMm;
                  const barHeight = isMissing 
                    ? chartHeight - chartPadding.top - chartPadding.bottom 
                    : Math.max(1, (rain / maxRainfall) * (chartHeight - chartPadding.top - chartPadding.bottom));
                  const y = isMissing 
                    ? chartPadding.top 
                    : chartHeight - chartPadding.bottom - barHeight;

                  // Severity color coding
                  const isHeavy = !isMissing && rain >= 7.5;
                  const isModerate = !isMissing && rain >= 2.5 && rain < 7.5;
                  const isLight = !isMissing && rain > 0 && rain < 2.5;

                  const barFill = isMissing 
                    ? '#F1F5F9' 
                    : isHeavy ? '#DC2626' : isModerate ? '#D97706' : isLight ? '#0284C7' : '#E2E8F0';

                  const isHovered = hoveredPoint?.time === point.time;

                  return (
                    <g 
                      key={idx}
                      onMouseEnter={() => setHoveredPoint(point)}
                      onMouseLeave={() => setHoveredPoint(null)}
                      className="cursor-pointer"
                    >
                      <rect
                        x={x}
                        y={y}
                        width={barWidth}
                        height={barHeight}
                        fill={isHovered ? '#102D27' : barFill}
                        stroke={isMissing ? '#CBD5E1' : 'none'}
                        strokeDasharray={isMissing ? '2 2' : 'none'}
                        rx={1}
                        className="transition-colors duration-150"
                      />
                      {/* Day separator tick marks every 24 hours */}
                      {idx % 24 === 0 && (
                        <text
                          x={x}
                          y={chartHeight - 6}
                          fontSize="9"
                          fill="#64748B"
                          fontFamily="monospace"
                          textAnchor="start"
                        >
                          {point.time?.split('T')[0]?.substring(5)}
                        </text>
                      )}
                    </g>
                  );
                })}
              </svg>
            </div>
            
            {/* Legend */}
            <div className="flex flex-wrap items-center justify-between text-[10px] text-[#64748B] pt-2 border-t border-[#E2E8E5] mt-2 font-mono">
              <div className="flex items-center gap-3">
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-xs bg-[#E2E8F0]" />
                  Dry (0.0 mm)
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-xs bg-[#0284C7]" />
                  Light (&lt; 2.5 mm/h)
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-xs bg-[#D97706]" />
                  Moderate (2.5 – 7.5 mm/h)
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-xs bg-[#DC2626]" />
                  Heavy (&gt; 7.5 mm/h)
                </span>
                <span className="flex items-center gap-1">
                  <span className="w-2.5 h-2.5 rounded-xs bg-[#F1F5F9] border border-dashed border-[#CBD5E1]" />
                  Missing / Unassimilated
                </span>
              </div>
              <span>Units: Millimeters per Hour (mm/h)</span>
            </div>
          </div>
        )}
      </div>

      {/* 6. Provenance & Dataset Governance Footer */}
      <div className="p-4 bg-[#F8FAF9] border-t border-[#E2E8E5] text-xs text-[#64748B] space-y-2">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-[#E2E8E5] pb-2 font-mono text-[11px]">
          <div className="flex items-center gap-2">
            <Database size={13} className="text-[#17483B]" />
            <span>Provider: <strong className="text-[#18201F]">{weatherData?.provider || 'Open-Meteo Historical Weather Archive'}</strong></span>
          </div>
          <div>
            <span>License: <strong className="text-[#18201F]">{weatherData?.license || 'CC BY 4.0'}</strong></span>
          </div>
          <div>
            <span>Retrieved: <strong className="text-[#18201F]">{weatherData?.retrievedAt ? new Date(weatherData.retrievedAt).toLocaleString() : 'Recent'}</strong></span>
          </div>
        </div>

        <p className="text-[11px] leading-relaxed text-[#64748B]">
          <strong>Audit Trace:</strong> This historical reanalysis data is ingested for retrospective spatial correlation and civil infrastructure risk calibration. 
          In accordance with Nirvaha Civic Integrity rules, historical weather records are completely decoupled from active grievance SLA computation and officer accountability scoring.
        </p>
      </div>
    </div>
  );
};
