export interface MapLngLat {
  lng: number;
  lat: number;
}

export interface MapMarker {
  id: string;
  lng: number;
  lat: number;
  label?: string;
  /** 标记颜色(默认红) */
  color?: string;
}

export interface MapLine {
  id: string;
  points: MapLngLat[];
  color?: string;
  width?: number;
}

export const EMPTY_FEATURE_COLLECTION = {
  type: 'FeatureCollection' as const,
  features: [],
};

/** 生成标记 DOM(圆形图钉 + 可选标签) */
export function createMarkerElement(marker: MapMarker): HTMLElement {
  const color = marker.color ?? '#F5222D';
  const el = document.createElement('div');
  el.className = 'maplibre-marker';
  el.style.position = 'relative';
  el.style.transform = 'translate(-50%, -100%)';
  el.style.cursor = 'pointer';

  const pin = document.createElement('div');
  pin.style.cssText = [
    'width:16px;height:16px;border-radius:50%;background:#ffffff;',
    `border:4px solid ${color};box-shadow:0 1px 4px rgba(0,0,0,0.35);`,
  ].join('');
  el.appendChild(pin);

  if (marker.label) {
    const label = document.createElement('div');
    label.textContent = marker.label;
    label.style.cssText = [
      'position:absolute;top:100%;left:50%;transform:translateX(-50%);',
      'margin-top:2px;padding:1px 6px;border-radius:3px;',
      'background:rgba(0,0,0,0.65);color:#fff;font-size:11px;line-height:16px;',
      'white-space:nowrap;pointer-events:none;',
    ].join('');
    el.appendChild(label);
  }
  return el;
}
