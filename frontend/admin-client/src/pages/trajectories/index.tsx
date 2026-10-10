import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Row, Empty, Card, message } from 'antd';
import { useQuery } from '@apollo/client';
import type { DeliveryTrajectoryVo, DeliveryWorkerVo } from '@aquarush/common';
import { BRAND_PRIMARY, COLOR_SUCCESS, COLOR_ERROR } from '../../config/theme';
import { DEFAULT_MAP_CENTER } from '../../lib/map/tianditu';
import { GET_DELIVERY_WORKERS_QUERY } from '../../graphql/queries/user.graphql';
import {
  GET_DELIVERY_TRAJECTORIES_QUERY,
  GET_DELIVERY_WORKER_LATEST_LOCATION_QUERY,
} from '../../graphql/queries/trajectory.graphql';
import {
  TrajectoryFilters,
  LiveLocationCard,
  TrajectoryStatsCard,
  ReplayControlsCard,
  TrajectoryMap,
} from './components';
import { useTrajectoryReplay } from './hooks/useTrajectoryReplay';
import { toPoints } from './utils';

/**
 * 配送轨迹管理(F-ADR-012,后台轨迹查看):
 * - 选择送水工 → 地图展示其实时位置(deliveryWorkerLatestLocation)与时间范围内轨迹(deliveryTrajectories);
 * - 轨迹折线 + 起点/终点标记 + 轨迹回放(可调速);
 * - 仅管理员可访问(后端 PERMISSION_DELIVERY_WORKER_READ 强制,R-AUTH-02)。
 */
const DeliveryTrajectoriesPage: React.FC = () => {
  const [selectedWorkerId, setSelectedWorkerId] = useState<string | undefined>();
  const [range, setRange] = useState<[string | null, string | null]>([null, null]);

  // 送水员列表(在线优先排序);分页接口取一页(下拉用,上限 100)
  const { data: workersData, loading: workersLoading } = useQuery<{
    deliveryWorkers: { content: readonly DeliveryWorkerVo[]; totalElements: number };
  }>(GET_DELIVERY_WORKERS_QUERY, {
    variables: { keyword: null, role: null, page: 0, size: 100 },
    fetchPolicy: 'cache-and-network',
  });
  const workers = useMemo(() => {
    const list = [...(workersData?.deliveryWorkers?.content ?? [])];
    list.sort((a, b) => (a.onlineStatus === 'ONLINE' ? -1 : 1) - (b.onlineStatus === 'ONLINE' ? -1 : 1));
    return list;
  }, [workersData]);

  // 送水工最新位置(实时)
  const { data: latestData, loading: latestLoading, refetch: refetchLatest } = useQuery<{
    deliveryWorkerLatestLocation?: DeliveryTrajectoryVo | null;
  }>(GET_DELIVERY_WORKER_LATEST_LOCATION_QUERY, {
    variables: selectedWorkerId ? { workerId: selectedWorkerId } : undefined,
    skip: !selectedWorkerId,
    fetchPolicy: 'cache-and-network',
  });

  // 轨迹序列(按时间范围;orderId 不指定,展示全部配送轨迹)
  const { data: trajData, loading: trajLoading, refetch: refetchTrajectories } = useQuery<{
    deliveryTrajectories: readonly DeliveryTrajectoryVo[];
  }>(GET_DELIVERY_TRAJECTORIES_QUERY, {
    variables: selectedWorkerId
      ? {
          workerId: selectedWorkerId,
          orderId: null,
          startTime: range[0],
          endTime: range[1],
        }
      : undefined,
    skip: !selectedWorkerId,
    fetchPolicy: 'cache-and-network',
  });

  const points = useMemo(() => toPoints(trajData?.deliveryTrajectories ?? []), [trajData]);
  const latest = latestData?.deliveryWorkerLatestLocation;

  // 回放控制(播放进度/状态/速度,定时器在组件内管理)
  const { playIndex, playing, replaySpeed, setPlayIndex, setPlaying, setReplaySpeed, stopPlayback, handlePlay } =
    useTrajectoryReplay(points);

  // 切换送水工时停止回放
  useEffect(() => stopPlayback, [stopPlayback, selectedWorkerId]);

  // 地图中心:优先最新位置,其次轨迹首点,兜底默认(轨迹坐标为 WGS-84,天地图直接展示)
  const mapCenter = useMemo(() => {
    if (latest) return { lng: latest.longitude, lat: latest.latitude };
    if (points.length > 0) return { lng: points[0].lng, lat: points[0].lat };
    return DEFAULT_MAP_CENTER;
  }, [latest, points]);

  // 完整轨迹折线
  const fullPath = useMemo(() => points.map((p) => ({ lng: p.lng, lat: p.lat })), [points]);

  // 回放折线(截止当前播放点的轨迹)
  const replayPath = useMemo(() => {
    if (playIndex < 0) return fullPath;
    return points.slice(0, playIndex + 1).map((p) => ({ lng: p.lng, lat: p.lat }));
  }, [points, playIndex, fullPath]);

  // 地图折线图层:完整轨迹(品牌蓝)+ 回放进度(红,后添加置于上层)
  const mapLines = useMemo(() => {
    const lines = [];
    if (points.length > 1) {
      lines.push({ id: 'full', points: fullPath, color: BRAND_PRIMARY, width: 6 });
    }
    if (playIndex >= 0 && replayPath.length > 0) {
      lines.push({ id: 'replay', points: replayPath, color: COLOR_ERROR, width: 6 });
    }
    return lines;
  }, [points, playIndex, replayPath, fullPath]);

  // 地图标记:当前位置 + 轨迹起点
  const mapMarkers = useMemo(() => {
    const markers = [];
    if (latest) {
      markers.push({ id: 'worker', lng: latest.longitude, lat: latest.latitude, label: '当前位置', color: COLOR_ERROR });
    } else if (points.length > 0) {
      // 非配送中无实时位置:以轨迹末点作为「最后位置」展示
      const last = points[points.length - 1];
      markers.push({ id: 'worker', lng: last.lng, lat: last.lat, label: '最后位置', color: COLOR_ERROR });
    }
    if (points.length > 1) {
      markers.push({ id: 'start', lng: points[0].lng, lat: points[0].lat, label: '起点', color: COLOR_SUCCESS });
    }
    return markers;
  }, [latest, points]);

  const handleRefresh = useCallback(async () => {
    if (!selectedWorkerId) return;
    try {
      await Promise.all([refetchLatest(), refetchTrajectories()]);
      message.success('位置与轨迹已刷新');
    } catch {
      message.error('获取轨迹数据失败');
    }
  }, [selectedWorkerId, refetchLatest, refetchTrajectories]);

  const selectedWorker = workers.find((w) => String(w.id) === String(selectedWorkerId));
  const isDelivering = points.some((p) => p.orderId);

  const timeText = (index: number) => points[index]?.time.replace('T', ' ');

  return (
    <div>
      <TrajectoryFilters
        workers={workers}
        workersLoading={workersLoading}
        selectedWorkerId={selectedWorkerId}
        onWorkerChange={(v) => {
          stopPlayback();
          setPlayIndex(-1);
          setSelectedWorkerId(v);
        }}
        onRangeChange={setRange}
        onRefresh={handleRefresh}
        refreshDisabled={!selectedWorkerId}
      />

      {!selectedWorkerId ? (
        <Card>
          <Empty description="请选择送水工查看实时位置与配送轨迹" />
        </Card>
      ) : (
        <>
          <Row gutter={16}>
            <LiveLocationCard latest={latest} loading={latestLoading} />
            <TrajectoryStatsCard
              pointsCount={points.length}
              workerOnline={selectedWorker?.onlineStatus === 'ONLINE'}
              isDelivering={isDelivering}
            />
            <ReplayControlsCard
              pointsCount={points.length}
              playIndex={playIndex}
              playing={playing}
              replaySpeed={replaySpeed}
              loading={trajLoading}
              onPlay={handlePlay}
              onSpeedChange={(v) => {
                setReplaySpeed(v);
                if (playing) {
                  stopPlayback();
                  setPlaying(true);
                }
              }}
              onSeek={(v) => {
                stopPlayback();
                setPlayIndex(v);
              }}
              timeText={timeText}
            />
          </Row>

          <TrajectoryMap center={mapCenter} markers={mapMarkers} lines={mapLines} pointsCount={points.length} />
        </>
      )}
    </div>
  );
};

export default DeliveryTrajectoriesPage;
