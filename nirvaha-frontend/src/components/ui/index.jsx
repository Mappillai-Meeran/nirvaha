import React from 'react';
import { FeedbackToast } from './FeedbackToast.jsx';
import { AlertCircle, RefreshCw, CheckCircle2, ShieldAlert, CloudRain } from 'lucide-react';
export { FeedbackToast };

export const Badge = ({ children, variant = 'default', className = '' }) => {
  const base = 'inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold font-mono uppercase tracking-wider border';
  const variants = {
    default: 'bg-[#F5F4EF] text-[#18201F] border-[#D9DEDB]',
    primary: 'bg-[#163A36]/10 text-[#163A36] border-[#163A36]/30',
    success: 'bg-[#3F7D5A]/10 text-[#3F7D5A] border-[#3F7D5A]/30',
    warning: 'bg-[#C58A32]/10 text-[#C58A32] border-[#C58A32]/30',
    danger: 'bg-[#B94A48]/10 text-[#B94A48] border-[#B94A48]/30',
    critical: 'bg-[#B94A48] text-white font-extrabold border-[#9E3937] shadow-sm',
    purple: 'bg-[#C87941]/10 text-[#C87941] border-[#C87941]/30',
    cyan: 'bg-[#2F6F68]/10 text-[#2F6F68] border-[#2F6F68]/30',
  };
  return <span className={`${base} ${variants[variant] || variants.default} ${className}`}>{children}</span>;
};

export const Card = ({ children, className = '' }) => (
  <div className={`bg-white border border-[#D9DEDB] rounded-lg p-4 text-[#18201F] shadow-sm ${className}`}>
    {children}
  </div>
);

export const LoadingState = ({ message = 'Syncing civic intelligence telemetry...' }) => (
  <div className="flex flex-col items-center justify-center p-12 text-[#66716E] space-y-3 bg-white border border-[#D9DEDB] rounded-lg">
    <RefreshCw className="w-6 h-6 animate-spin text-[#163A36]" />
    <span className="text-xs font-mono tracking-wider">{message}</span>
  </div>
);

export const ErrorState = ({ message = 'Failed to load telemetry data', onRetry }) => (
  <div className="bg-[#B94A48]/5 border border-[#B94A48]/30 rounded-lg p-5 text-center space-y-3 max-w-md mx-auto">
    <AlertCircle className="w-8 h-8 text-[#B94A48] mx-auto" />
    <h3 className="text-sm font-bold text-[#B94A48]">System Telemetry Error</h3>
    <p className="text-xs text-[#18201F]/80 leading-relaxed">{message}</p>
    {onRetry && (
      <button
        onClick={onRetry}
        className="mt-1 inline-flex items-center px-3 py-1.5 bg-[#B94A48] hover:bg-[#9E3937] text-white text-xs font-bold uppercase tracking-wider rounded transition"
      >
        <RefreshCw className="w-3.5 h-3.5 mr-1.5" />
        Retry Operation
      </button>
    )}
  </div>
);

export const EmptyState = ({ title = 'No Active Telemetry', description = 'There are currently no records matching this filter criteria.' }) => (
  <div className="border border-dashed border-[#D9DEDB] rounded-lg p-8 text-center space-y-2 text-[#66716E] bg-white/60">
    <CheckCircle2 className="w-8 h-8 text-[#66716E] mx-auto" />
    <h4 className="text-xs font-bold text-[#18201F] uppercase tracking-wider">{title}</h4>
    <p className="text-xs text-[#66716E] max-w-sm mx-auto">{description}</p>
  </div>
);

export const WeatherIndicator = ({ weatherData, weatherContext }) => {
  if (!weatherData || !weatherData.available) {
    return (
      <div className="bg-[#C58A32]/10 border border-[#C58A32]/30 rounded p-2.5 text-xs text-[#C58A32] space-y-1">
        <div className="flex items-center space-x-2 font-bold text-[#C58A32] text-[11px] uppercase tracking-wider">
          <ShieldAlert className="w-3.5 h-3.5" />
          <span>Live Weather Telemetry Status</span>
        </div>
        <p className="text-[#66716E] text-[11px] leading-relaxed">
          {weatherContext || weatherData?.sourceMessage || 'Real-time meteorological service unavailable; calculating risk from spatial history.'}
        </p>
      </div>
    );
  }

  const obsTime = weatherData.observedAt || weatherData.timestamp
    ? new Date(weatherData.observedAt || weatherData.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : 'Live';
  const retrievedTime = weatherData.retrievedAt
    ? new Date(weatherData.retrievedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : null;

  return (
    <div className="bg-white border border-[#D9DEDB] rounded p-2.5 text-xs space-y-1.5">
      <div className="flex items-center justify-between font-bold text-[#163A36] text-[11px] uppercase tracking-wider">
        <span className="flex items-center space-x-1.5">
          <CloudRain className="w-3.5 h-3.5 text-[#2F6F68]" />
          <span>{weatherData.sourceMessage ? weatherData.sourceMessage.split('(')[0].trim() : 'Open-Meteo Weather'}</span>
        </span>
        <span className="font-mono text-[#18201F] text-xs font-bold">
          {weatherData.temperature != null ? `${weatherData.temperature}°C` : '--'}
        </span>
      </div>
      <div className="text-[10px] text-[#66716E] flex justify-between items-center">
        <span>Obs: <strong className="text-[#18201F]">{obsTime}</strong>{weatherData.isCached ? ' (cached)' : ''}</span>
        {retrievedTime && <span>Sync: <strong className="text-[#18201F]">{retrievedTime}</strong></span>}
        <span>Hum: <strong className="text-[#18201F]">{weatherData.humidity != null ? `${weatherData.humidity}%` : '--'}</strong></span>
      </div>
      <div className="grid grid-cols-2 gap-2 text-[10px] text-[#66716E] font-mono pt-1 border-t border-[#D9DEDB]">
        <div>15m Rain: <span className="text-[#18201F] font-bold">{weatherData.recentRainfall != null ? `${weatherData.recentRainfall} mm` : '--'}</span></div>
        <div>24h Rain: <span className="text-[#18201F] font-bold">{weatherData.forecastRainfall != null ? `${weatherData.forecastRainfall} mm` : '--'}</span></div>
      </div>
    </div>
  );
};
