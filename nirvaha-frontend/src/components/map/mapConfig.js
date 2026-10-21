/**
 * Leaflet map configuration for Nirvaha GIS views.
 * OpenStreetMap raster tile specifications for Puducherry municipal jurisdiction.
 */

export const PUDUCHERRY_CENTER = [11.9350, 79.8300];

export const MAP_CONFIG = {
  tileUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  maxZoom: 19,
  minZoom: 10,
  defaultCenter: PUDUCHERRY_CENTER,
  defaultZoom: 13,
};
