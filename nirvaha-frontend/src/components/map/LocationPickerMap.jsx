import React, { useEffect, useMemo } from 'react';
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import { MapPin } from 'lucide-react';
import { renderToStaticMarkup } from 'react-dom/server';

const MAP_CONFIG = {
  url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
};

const pinIcon = new L.DivIcon({
  className: 'location-pin-wrapper',
  html: renderToStaticMarkup(<span className="location-pin"><MapPin size={11} fill="currentColor" /></span>),
  iconSize: [20, 25],
  iconAnchor: [10, 23],
});

const MapClickHandler = ({ onChange }) => {
  useMapEvents({ click: (event) => onChange(event.latlng.lat, event.latlng.lng) });
  return null;
};

const MapViewUpdater = ({ position }) => {
  const map = useMap();
  useEffect(() => {
    if (position && position[0] && position[1]) {
      map.setView(position, map.getZoom(), { animate: true });
    }
  }, [position, map]);
  return null;
};

export const LocationPickerMap = ({ latitude, longitude, onChange }) => {
  const position = useMemo(() => [latitude || 11.935, longitude || 79.83], [latitude, longitude]);

  return (
    <MapContainer 
      center={position} 
      zoom={15} 
      scrollWheelZoom={false} 
      className="location-map"
      style={{ width: '100%', height: '315px', position: 'relative', zIndex: 1 }}
    >
      <TileLayer attribution={MAP_CONFIG.attribution} url={MAP_CONFIG.url} maxZoom={19} />
      <MapClickHandler onChange={onChange} />
      <MapViewUpdater position={position} />
      <Marker
        position={position}
        icon={pinIcon}
        draggable
        eventHandlers={{ dragend: (event) => { const next = event.target.getLatLng(); onChange(next.lat, next.lng); } }}
      />
    </MapContainer>
  );
};
