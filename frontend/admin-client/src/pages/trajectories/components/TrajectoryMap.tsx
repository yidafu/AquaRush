import React from 'react';
import { Card } from '@/ui';
import { EnvironmentOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import MapLibreMap from '../../../components/MapLibreMap';
import type { MapLine, MapMarker } from '../../../components/MapLibreMap';

// 说明:坐标浮层底色 rgba(0,0,0,0.6) 为本组件局部色值,共享 token 无对应值,保留字面量(报告 (a))
const styles = stylex.create({
  card: {
    marginTop: '16px',
    overflow: 'hidden',
  },
  mapWrap: {
    position: 'relative',
    height: '480px',
  },
  coordOverlay: {
    position: 'absolute',
    bottom: '8px',
    left: '8px',
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    color: tokens.onBrand,
    paddingTop: '4px',
    paddingRight: '8px',
    paddingBottom: '4px',
    paddingLeft: '8px',
    borderRadius: '4px',
    fontSize: '12px',
    zIndex: 1000,
    pointerEvents: 'none',
  },
});

export interface TrajectoryMapProps {
  center: { lng: number; lat: number };
  markers: MapMarker[];
  lines: MapLine[];
  pointsCount: number;
}

/** 轨迹地图(轨迹折线 + 起点/终点标记 + 坐标浮层,trajectories/index.tsx 拆分) */
const TrajectoryMap: React.FC<TrajectoryMapProps> = ({ center, markers, lines, pointsCount }) => (
  <Card size="small" {...stylex.props(styles.card)}>
    <div {...stylex.props(styles.mapWrap)}>
      <MapLibreMap
        center={center}
        zoom={14}
        height={480}
        markers={markers}
        lines={lines}
      />
      <div {...stylex.props(styles.coordOverlay)}>
        <EnvironmentOutlined /> {center.lat.toFixed(4)}, {center.lng.toFixed(4)} · 共 {pointsCount} 个轨迹点
      </div>
    </div>
  </Card>
);

export default TrajectoryMap;
