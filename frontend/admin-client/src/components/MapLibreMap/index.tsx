import React, { useCallback, useEffect, useRef } from 'react';
import {
  Map as MaplibreMap,
  Marker,
  NavigationControl,
  AttributionControl,
  GeoJSONSource,
} from 'maplibre-gl';
import type { MapMouseEvent } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { TIANDITU_KEY, DEFAULT_MAP_CENTER, getTiandituStyle } from '../../lib/map/tianditu';

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

interface MapLibreMapProps {
  /** 地图中心(存储/接口坐标统一 WGS-84,直接传入;与天地图底图一致) */
  center?: MapLngLat;
  zoom?: number;
  height?: number | string;
  markers?: MapMarker[];
  lines?: MapLine[];
  onClick?: (lngLat: MapLngLat) => void;
  /** 地图 style 加载完成回调(可用于标记"地图就绪") */
  onMapLoad?: (map: MaplibreMap) => void;
  /** 禁用地图交互(不可点选) */
  disabled?: boolean;
  style?: React.CSSProperties;
  className?: string;
}

const EMPTY_FEATURE_COLLECTION = {
  type: 'FeatureCollection' as const,
  features: [],
};

/** 生成标记 DOM(圆形图钉 + 可选标签) */
function createMarkerElement(marker: MapMarker): HTMLElement {
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

/**
 * MapLibre + 天地图底图的轻量封装组件:
 * - 底图来自 getTiandituStyle()(矢量 + 注记);
 * - markers 渲染为 HTML DOM 标记,lines 渲染为 GeoJSON line 图层;
 * - props 变化时增量同步(标记增删移动、折线 setData),支持点击回调。
 */
const MapLibreMap: React.FC<MapLibreMapProps> = ({
  center,
  zoom = 16,
  height = 350,
  markers = [],
  lines = [],
  onClick,
  onMapLoad,
  disabled = false,
  style,
  className,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<MaplibreMap | null>(null);
  const loadedRef = useRef(false);
  const markerInstancesRef = useRef<Map<string, Marker>>(new Map());
  const lineSourceIdsRef = useRef<Set<string>>(new Set());
  const lastCenterRef = useRef<string>('');
  const resizeObserverRef = useRef<ResizeObserver | null>(null);

  // 最新 props 引用,供 sync 在任意时机读取(避免闭包过期)
  const propsRef = useRef({ center, zoom, markers, lines, onClick, disabled });
  propsRef.current = { center, zoom, markers, lines, onClick, disabled };

  const syncMarkers = useCallback((map: MaplibreMap, list: MapMarker[]) => {
    const instances = markerInstancesRef.current;
    const seen = new Set<string>();

    for (const marker of list) {
      seen.add(marker.id);
      const existing = instances.get(marker.id);
      if (existing) {
        existing.setLngLat([marker.lng, marker.lat]);
      } else {
        const instance = new Marker({
          element: createMarkerElement(marker),
        })
          .setLngLat([marker.lng, marker.lat])
          .addTo(map);
        instances.set(marker.id, instance);
      }
    }

    for (const [id, instance] of instances) {
      if (!seen.has(id)) {
        instance.remove();
        instances.delete(id);
      }
    }
  }, []);

  const syncLines = useCallback((map: MaplibreMap, list: MapLine[]) => {
    const sourceIds = lineSourceIdsRef.current;

    for (const line of list) {
      const sourceId = `line-${line.id}`;
      const layerId = `line-layer-${line.id}`;
      sourceIds.add(sourceId);

      if (!map.getSource(sourceId)) {
        // 首次创建即写入线段数据(否则首帧/缓存命中场景折线不显示)
        map.addSource(sourceId, {
          type: 'geojson',
          data:
            line.points.length > 1
              ? {
                  type: 'Feature',
                  properties: {},
                  geometry: {
                    type: 'LineString',
                    coordinates: line.points.map((p) => [p.lng, p.lat]),
                  },
                }
              : EMPTY_FEATURE_COLLECTION,
        });
        map.addLayer({
          id: layerId,
          type: 'line',
          source: sourceId,
          layout: { 'line-cap': 'round', 'line-join': 'round' },
          paint: {
            'line-color': line.color ?? '#2F80ED',
            'line-width': line.width ?? 4,
          },
        });
      } else {
        const source = map.getSource(sourceId) as GeoJSONSource;
        source.setData(
          line.points.length > 1
            ? {
                type: 'Feature',
                properties: {},
                geometry: {
                  type: 'LineString',
                  coordinates: line.points.map((p) => [p.lng, p.lat]),
                },
              }
            : EMPTY_FEATURE_COLLECTION,
        );
        const layer = map.getLayer(layerId);
        if (layer) {
          map.setPaintProperty(layerId, 'line-color', line.color ?? '#2F80ED');
          map.setPaintProperty(layerId, 'line-width', line.width ?? 4);
        }
      }
    }

    // 清理已移除的线
    for (const sourceId of [...sourceIds]) {
      if (!list.some((l) => `line-${l.id}` === sourceId)) {
        const layerId = sourceId.replace(/^line-/, 'line-layer-');
        if (map.getLayer(layerId)) map.removeLayer(layerId);
        if (map.getSource(sourceId)) map.removeSource(sourceId);
        sourceIds.delete(sourceId);
      }
    }
  }, []);

  /** 同步全部数据(标记/折线/中心) */
  const syncAll = useCallback(() => {
    const map = mapRef.current;
    if (!map || !loadedRef.current) return;
    const { center: c, zoom: z, markers: m, lines: l } = propsRef.current;

    syncMarkers(map, m);
    syncLines(map, l);

    const centerKey = c ? `${c.lng.toFixed(6)},${c.lat.toFixed(6)}` : '';
    if (centerKey && centerKey !== lastCenterRef.current) {
      lastCenterRef.current = centerKey;
      map.jumpTo({ center: [c!.lng, c!.lat], zoom: z });
    }
  }, [syncMarkers, syncLines]);

  // 初始化地图(一次)
  useEffect(() => {
    const container = containerRef.current;
    if (!container || mapRef.current) return;
    // 捕获 ref 快照供清理使用(ref 对象仅在本挂载周期内被同步函数变更)
    const instances = markerInstancesRef.current;
    const sourceIds = lineSourceIdsRef.current;

    if (!TIANDITU_KEY) {
      console.warn('[MapLibre] 未配置 VITE_TIANDITU_KEY,天地图底图不可用');
      container.innerHTML =
        '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#999;font-size:13px;">未配置 VITE_TIANDITU_KEY,天地图底图不可用</div>';
      return;
    }

    const map = new MaplibreMap({
      container,
      style: getTiandituStyle(),
      center: [center?.lng ?? DEFAULT_MAP_CENTER.lng, center?.lat ?? DEFAULT_MAP_CENTER.lat],
      zoom,
      attributionControl: false,
    });
    mapRef.current = map;

    map.addControl(new NavigationControl({ showCompass: true }), 'top-right');
    map.addControl(
      new AttributionControl({ compact: true }),
      'bottom-right',
    );

    map.on('load', () => {
      loadedRef.current = true;
      syncAll();
      onMapLoad?.(map);
    });

    if (onClick) {
      map.on('click', (e: MapMouseEvent) => {
        const { onClick: cb, disabled: isDisabled } = propsRef.current;
        if (cb && !isDisabled) {
          cb({ lng: e.lngLat.lng, lat: e.lngLat.lat });
        }
      });
    }

    // 容器尺寸变化时 resize(折叠/切 Tab 场景)
    if (typeof ResizeObserver !== 'undefined') {
      const ro = new ResizeObserver(() => map.resize());
      ro.observe(container);
      resizeObserverRef.current = ro;
    }

    return () => {
      resizeObserverRef.current?.disconnect();
      resizeObserverRef.current = null;
      for (const instance of instances.values()) {
        instance.remove();
      }
      instances.clear();
      sourceIds.clear();
      lastCenterRef.current = '';
      loadedRef.current = false;
      map.remove();
      mapRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 每次渲染后同步数据(标记/折线变化驱动)
  useEffect(() => {
    syncAll();
  });

  return (
    <div
      ref={containerRef}
      className={className}
      style={{
        position: 'relative',
        width: '100%',
        height,
        ...style,
      }}
    />
  );
};

export default MapLibreMap;
