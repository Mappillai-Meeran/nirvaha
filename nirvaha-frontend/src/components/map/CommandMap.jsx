import React, { useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { ShieldAlert, RefreshCcw } from 'lucide-react';

// Fix Leaflet marker icon asset paths
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});
import { MAP_CONFIG, PUDUCHERRY_CENTER } from './mapConfig';

export const CommandMap = ({ 
  incidents = [], 
  wards = [], 
  signals = [],
  selectedWard = null, 
  onSelectWard, 
  onSelectIncident,
  onSelectSignal,
  activeLayers = { incidents: true, risk: true, hotspots: true }
}) => {
  const [tileError, setTileError] = useState(false);

  const handleTileError = () => {
    setTileError(true);
  };

  return (
    <div className="w-full h-full min-h-[440px] rounded-lg overflow-hidden border border-[#D9DEDB] bg-[#e5e3df] relative z-1 isolate flex flex-col shadow-sm">
      {tileError ? (
        <div className="flex-1 flex flex-col items-center justify-center p-8 bg-white text-center space-y-3 border border-[#D9DEDB] rounded-lg">
          <ShieldAlert className="w-10 h-10 text-[#C58A32]" />
          <h3 className="text-sm font-bold text-[#18201F] uppercase tracking-wider">Geographic Tile Server Unavailable</h3>
          <p className="text-xs text-[#66716E] max-w-md leading-relaxed">
            OpenStreetMap tile services could not be loaded in the current network environment. Civic telemetry data, risk scores, and incident queues remain fully operational.
          </p>
          <button
            onClick={() => setTileError(false)}
            className="mt-2 inline-flex items-center px-3 py-1.5 bg-[#163A36] hover:bg-[#2F6F68] text-white text-xs font-bold uppercase tracking-wider rounded transition shadow-sm"
          >
            <RefreshCcw className="w-3.5 h-3.5 mr-1.5" />
            Retry Tile Load
          </button>
        </div>
      ) : (
        <MapContainer 
          center={PUDUCHERRY_CENTER} 
          zoom={13} 
          style={{ width: '100%', height: '100%', minHeight: '440px', background: '#e5e3df' }}
        >
          <TileLayer
            attribution={MAP_CONFIG.attribution}
            url={MAP_CONFIG.tileUrl}
            eventHandlers={{
              tileerror: handleTileError
            }}
          />

          {/* Render Ward Risk Boundaries / Centers */}
          {activeLayers.risk && wards.map((ward) => {
            const isSelected = selectedWard && selectedWard.wardId === ward.wardId;
            const riskColor = 
              ward.riskLevel === 'CRITICAL' ? '#B94A48' :
              ward.riskLevel === 'HIGH' ? '#C58A32' :
              ward.riskLevel === 'MODERATE' ? '#D97706' : '#3F7D5A';

            return (
              <React.Fragment key={`ward-${ward.wardId}`}>
                <Circle
                  center={[ward.latitude || 11.9350, ward.longitude || 79.8300]}
                  radius={750}
                  pathOptions={{
                    color: riskColor,
                    fillColor: riskColor,
                    fillOpacity: isSelected ? 0.28 : 0.14,
                    weight: isSelected ? 2.5 : 1.2,
                    dashArray: isSelected ? undefined : '4, 4'
                  }}
                  eventHandlers={{
                    click: () => onSelectWard && onSelectWard(ward)
                  }}
                >
                  <Popup className="custom-popup">
                    <div className="p-2 text-[#18201F] text-xs space-y-1">
                      <div className="font-bold border-b border-[#D9DEDB] pb-1 text-xs">{ward.wardName} (Ward #{ward.wardNumber})</div>
                      {ward.isSimulated && (
                        <div className="text-[10px] text-amber-700 font-bold bg-amber-100 px-1 py-0.5 rounded text-center">
                          ● SIMULATED PREDICTIVE STRESS
                        </div>
                      )}
                      <div>Risk Score: <strong>{ward.riskScore100 || (ward.riskScore * 100).toFixed(0)} / 100</strong></div>
                      <div>Level: <strong style={{ color: riskColor }}>{ward.riskLevel}</strong></div>
                      <div>Top Category: <strong>{ward.topRiskCategory}</strong></div>
                      <div className="text-[10px] text-[#66716E] pt-1">Click ward polygon to view risk breakdown</div>
                    </div>
                  </Popup>
                </Circle>
              </React.Fragment>
            );
          })}

          {/* Render Phase 13 Civic Intelligence Hotspots / Signals */}
          {activeLayers.hotspots && signals.filter(s => s.latitude && s.longitude).map((sig) => {
            const sigColor = 
              sig.signalType === 'HOTSPOT' ? '#C87941' :
              sig.signalType === 'EMERGING_ISSUE' ? '#B94A48' :
              sig.signalType === 'RECURRING_PROBLEM' ? '#C58A32' : '#2F6F68';

            return (
              <Circle
                key={`sig-${sig.id}`}
                center={[sig.latitude, sig.longitude]}
                radius={sig.radiusMeters || 500}
                pathOptions={{
                  color: sigColor,
                  fillColor: sigColor,
                  fillOpacity: 0.20,
                  weight: 1.8,
                  dashArray: '5, 5'
                }}
                eventHandlers={{
                  click: () => onSelectSignal && onSelectSignal(sig)
                }}
              >
                <Popup className="custom-popup">
                  <div className="p-2 text-[#18201F] text-xs max-w-xs space-y-1">
                    <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-1">
                      <span className="font-bold text-[10px] uppercase font-mono tracking-wider" style={{ color: sigColor }}>
                        Nirvaha {sig.signalType}
                      </span>
                      <span className="font-mono text-[9px] text-[#66716E]">{sig.modelVersion || 'v1'}</span>
                    </div>
                    <div className="font-bold text-xs">{sig.category} • {sig.areaName}</div>
                    <div className="text-[11px] text-[#18201F] leading-snug">{sig.explanation}</div>
                    <div className="text-[10px] text-[#66716E] pt-1 border-t border-[#D9DEDB]">
                      <div>Incidents: <strong>{sig.incidentCount}</strong> ({sig.unresolvedCount} unresolved)</div>
                      <div>Window: <strong>{sig.timeWindow}</strong></div>
                    </div>
                  </div>
                </Popup>
              </Circle>
            );
          })}

          {/* Render Incidents */}
          {activeLayers.incidents && incidents.map((inc) => (
            <Marker
              key={`inc-${inc.id}`}
              position={[inc.latitude, inc.longitude]}
              eventHandlers={{
                click: () => onSelectIncident && onSelectIncident(inc)
              }}
            >
              <Popup>
                <div className="p-2 text-[#18201F] text-xs space-y-1">
                  <div className="flex items-center justify-between border-b border-[#D9DEDB] pb-1">
                    <span className="font-mono text-[10px] text-[#66716E] font-bold">CASE #{inc.id}</span>
                    <span className="font-mono text-[9px] text-[#163A36] font-semibold">{inc.originalLanguage || 'EN'}</span>
                  </div>
                  <h4 className="font-bold text-xs text-[#18201F] leading-snug">{inc.title}</h4>
                  <p className="text-[11px] text-[#66716E]">{inc.category} • Severity: <strong>{inc.severity}</strong></p>
                  {inc.normalizedDescription && (
                    <div className="text-[10px] bg-[#F5F4EF] p-1 rounded font-mono text-[#18201F] line-clamp-2">
                      {inc.normalizedDescription}
                    </div>
                  )}
                  <div className="font-semibold text-[#18201F] text-[11px] pt-1 border-t border-[#D9DEDB]">
                    Status: <span className="text-[#163A36]">{inc.status}</span>
                  </div>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      )}
    </div>
  );
};
