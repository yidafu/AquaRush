import React from 'react';
import { Card } from 'antd';
import { EnvironmentOutlined } from '@ant-design/icons';
import MapLibreMap from '../../../components/MapLibreMap';
import type { MapLine, MapMarker } from '../../../components/MapLibreMap';

export interface TrajectoryMapProps {
  center: { lng: number; lat: number };
  markers: MapMarker[];
  lines: MapLine[];
  pointsCount: number;
}

/** 轨迹地图(轨迹折线 + 起点/终点标记 + 坐标浮层,trajectories/index.tsx 拆分) */
const TrajectoryMap: React.FC<TrajectoryMapProps> = ({ center, markers, lines, pointsCount }) => (
  <Card size="small" style={{ marginTop: 16, overflow: 'hidden' }}>
    <div style={{ position: 'relative', height: 480 }}>
      <MapLibreMap
        center={center}
        zoom={14}
        height={480}
        markers={markers}
        lines={lines}
      />
      <div
        style={{
          position: 'absolute',
          bottom: 8,
          left: 8,
          background: 'rgba(0,0,0,0.6)',
          color: '#fff',
          padding: '4px 8px',
          borderRadius: 4,
          fontSize: 12,
          zIndex: 1000,
          pointerEvents: 'none',
        }}
      >
        <EnvironmentOutlined /> {center.lat.toFixed(4)}, {center.lng.toFixed(4)} · 共 {pointsCount} 个轨迹点
      </div>
    </div>
  </Card>
);

export default TrajectoryMap;
