import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Button, Card, Col, DatePicker, Descriptions, Empty, Row, Select, Slider, Space, Spin, Statistic, message, Tag, Typography } from 'antd';
import {
  AimOutlined,
  EnvironmentOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { useQuery } from '@apollo/client';
import type { DeliveryTrajectory, DeliveryWorker } from '@aquarush/common';
import MapLibreMap from '../../components/MapLibreMap';
import type { MapLine, MapMarker } from '../../components/MapLibreMap';
import { DEFAULT_MAP_CENTER } from '../../lib/map/tianditu';
import { GET_DELIVERY_WORKERS_QUERY } from '../../graphql/queries/user.graphql';
import {
  GET_DELIVERY_TRAJECTORIES_QUERY,
  GET_DELIVERY_WORKER_LATEST_LOCATION_QUERY,
} from '../../graphql/queries/trajectory.graphql';

const { RangePicker } = DatePicker;

interface TrajectoryPoint {
  lat: number;
  lng: number;
  time: string;
  orderId: string;
}

/** 将后端轨迹点转换为地图坐标序列 */
const toPoints = (list: readonly DeliveryTrajectory[]): TrajectoryPoint[] =>
  list.map((t) => ({
    lat: t.latitude,
    lng: t.longitude,
    time: t.reportedAt,
    orderId: t.orderId,
  }));

/**
 * 配送轨迹管理(F-ADR-012,后台轨迹查看):
 * - 选择送水工 → 地图展示其实时位置(deliveryWorkerLatestLocation)与时间范围内轨迹(deliveryTrajectories);
 * - 轨迹折线 + 起点/终点标记 + 轨迹回放(可调速);
 * - 仅管理员可访问(后端 PERMISSION_DELIVERY_WORKER_READ 强制,R-AUTH-02)。
 */
const DeliveryTrajectoriesPage: React.FC = () => {
  const [selectedWorkerId, setSelectedWorkerId] = useState<string | undefined>();
  const [range, setRange] = useState<[string | null, string | null]>([null, null]);
  const [playIndex, setPlayIndex] = useState(-1);
  const [playing, setPlaying] = useState(false);
  const [replaySpeed, setReplaySpeed] = useState(2);
  const playTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // 送水员列表(在线优先排序)
  const { data: workersData, loading: workersLoading } = useQuery<{ deliveryWorkers: readonly DeliveryWorker[] }>(
    GET_DELIVERY_WORKERS_QUERY,
    { fetchPolicy: 'cache-and-network' },
  );
  const workers = useMemo(() => {
    const list = [...(workersData?.deliveryWorkers ?? [])];
    list.sort((a, b) => (a.onlineStatus === 'ONLINE' ? -1 : 1) - (b.onlineStatus === 'ONLINE' ? -1 : 1));
    return list;
  }, [workersData]);

  // 送水工最新位置(实时)
  const { data: latestData, loading: latestLoading, refetch: refetchLatest } = useQuery<{
    deliveryWorkerLatestLocation?: DeliveryTrajectory | null;
  }>(GET_DELIVERY_WORKER_LATEST_LOCATION_QUERY, {
    variables: selectedWorkerId ? { workerId: selectedWorkerId } : undefined,
    skip: !selectedWorkerId,
    fetchPolicy: 'cache-and-network',
  });

  // 轨迹序列(按时间范围;orderId 不指定,展示全部配送轨迹)
  const { data: trajData, loading: trajLoading, refetch: refetchTrajectories } = useQuery<{
    deliveryTrajectories: readonly DeliveryTrajectory[];
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

  // 地图折线图层:完整轨迹(蓝)+ 回放进度(红,后添加置于上层)
  const mapLines = useMemo<MapLine[]>(() => {
    const lines: MapLine[] = [];
    if (points.length > 1) {
      lines.push({ id: 'full', points: fullPath, color: '#2F80ED', width: 6 });
    }
    if (playIndex >= 0 && replayPath.length > 0) {
      lines.push({ id: 'replay', points: replayPath, color: '#EB5757', width: 6 });
    }
    return lines;
  }, [points, playIndex, replayPath, fullPath]);

  // 地图标记:当前位置 + 轨迹起点
  const mapMarkers = useMemo<MapMarker[]>(() => {
    const markers: MapMarker[] = [];
    if (latest) {
      markers.push({ id: 'worker', lng: latest.longitude, lat: latest.latitude, label: '当前位置', color: '#F5222D' });
    } else if (points.length > 0) {
      // 非配送中无实时位置:以轨迹末点作为「最后位置」展示
      const last = points[points.length - 1];
      markers.push({ id: 'worker', lng: last.lng, lat: last.lat, label: '最后位置', color: '#F5222D' });
    }
    if (points.length > 1) {
      markers.push({ id: 'start', lng: points[0].lng, lat: points[0].lat, label: '起点', color: '#52C41A' });
    }
    return markers;
  }, [latest, points]);

  const stopPlayback = useCallback(() => {
    setPlaying(false);
    if (playTimerRef.current) {
      clearInterval(playTimerRef.current);
      playTimerRef.current = null;
    }
  }, []);

  const handlePlay = useCallback(() => {
    if (points.length === 0) return;
    if (playing) {
      stopPlayback();
      return;
    }
    setPlayIndex(0);
    setPlaying(true);
    playTimerRef.current = setInterval(() => {
      setPlayIndex((prev) => {
        if (prev >= points.length - 1) {
          setPlaying(false);
          if (playTimerRef.current) {
            clearInterval(playTimerRef.current);
            playTimerRef.current = null;
          }
          return prev;
        }
        return prev + 1;
      });
    }, 1000 / replaySpeed);
  }, [points, playing, replaySpeed, stopPlayback]);

  // 组件卸载/切换送水工时停止回放
  useEffect(() => stopPlayback, [stopPlayback, selectedWorkerId]);

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

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col>
            <Space>
              <Typography.Text strong>送水工:</Typography.Text>
              <Select
                placeholder="选择送水工查看轨迹"
                style={{ width: 240 }}
                loading={workersLoading}
                value={selectedWorkerId}
                onChange={(v) => {
                  stopPlayback();
                  setPlayIndex(-1);
                  setSelectedWorkerId(v);
                }}
                options={workers.map((w) => ({
                  value: String(w.id),
                  label: `${w.name}（${w.phone ?? ''}）${w.onlineStatus === 'ONLINE' ? ' · 在线' : ''}`,
                }))}
              />
            </Space>
          </Col>
          <Col>
            <Space>
              <Typography.Text strong>时间范围:</Typography.Text>
              <RangePicker
                showTime
                allowClear
                placeholder={['开始时间', '结束时间']}
                onChange={(dates) => {
                  setRange([
                    dates?.[0] ? dates[0].format('YYYY-MM-DDTHH:mm:ss') : null,
                    dates?.[1] ? dates[1].format('YYYY-MM-DDTHH:mm:ss') : null,
                  ]);
                }}
              />
            </Space>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh} disabled={!selectedWorkerId}>
              刷新
            </Button>
          </Col>
        </Row>
      </Card>

      {!selectedWorkerId ? (
        <Card>
          <Empty description="请选择送水工查看实时位置与配送轨迹" />
        </Card>
      ) : (
        <>
          <Row gutter={16}>
            <Col span={8}>
              <Card size="small" title="实时位置">
                <Spin spinning={latestLoading}>
                  {latest ? (
                    <Descriptions column={1} size="small">
                      <Descriptions.Item label="当前坐标">
                        <AimOutlined /> {latest.longitude.toFixed(6)}, {latest.latitude.toFixed(6)}
                      </Descriptions.Item>
                      <Descriptions.Item label="最近上报">{latest.reportedAt.replace('T', ' ')}</Descriptions.Item>
                      <Descriptions.Item label="关联订单">#{latest.orderId}</Descriptions.Item>
                      <Descriptions.Item label="定位精度">
                        {latest.accuracy != null ? `${latest.accuracy.toFixed(1)} 米` : '未知'}
                      </Descriptions.Item>
                    </Descriptions>
                  ) : (
                    <Typography.Text type="secondary">暂无实时位置(非配送中)</Typography.Text>
                  )}
                </Spin>
              </Card>
            </Col>
            <Col span={8}>
              <Card size="small" title="轨迹统计">
                <Row gutter={16}>
                  <Col span={12}>
                    <Statistic title="轨迹点数" value={points.length} suffix="个" />
                  </Col>
                  <Col span={12}>
                    <Statistic title="送水工状态" valueRender={() => (
                      <Tag color={selectedWorker?.onlineStatus === 'ONLINE' ? 'success' : 'default'}>
                        {selectedWorker?.onlineStatus === 'ONLINE' ? '在线' : '离线'}
                      </Tag>
                    )} />
                  </Col>
                </Row>
                {isDelivering ? (
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    <SyncOutlined spin /> 存在配送中订单,位置持续更新
                  </Typography.Text>
                ) : null}
              </Card>
            </Col>
            <Col span={8}>
              <Card size="small" title="轨迹回放">
                <Spin spinning={trajLoading}>
                  {points.length > 0 ? (
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <Space>
                        <Button
                          size="small"
                          icon={playing ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                          onClick={handlePlay}
                          disabled={points.length < 2}
                        >
                          {playing ? '暂停' : '播放'}
                        </Button>
                        <Select
                          size="small"
                          style={{ width: 100 }}
                          value={replaySpeed}
                          onChange={(v) => {
                            setReplaySpeed(v);
                            if (playing) {
                              stopPlayback();
                              setPlaying(true);
                              setPlayIndex((prev) => prev);
                            }
                          }}
                          options={[
                            { value: 1, label: '1x 慢速' },
                            { value: 2, label: '2x 标准' },
                            { value: 5, label: '5x 快速' },
                            { value: 10, label: '10x 极速' },
                          ]}
                        />
                      </Space>
                      <Slider
                        min={0}
                        max={Math.max(points.length - 1, 0)}
                        value={playIndex >= 0 ? playIndex : 0}
                        onChange={(v) => {
                          stopPlayback();
                          setPlayIndex(v);
                        }}
                        tooltip={{ formatter: (v) => points[v ?? 0]?.time.replace('T', ' ') }}
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {playIndex >= 0 ? `播放至 ${points[playIndex].time.replace('T', ' ')}` : '点击「播放」按时间顺序回放轨迹'}
                      </Typography.Text>
                    </Space>
                  ) : (
                    <Typography.Text type="secondary">暂无轨迹数据</Typography.Text>
                  )}
                </Spin>
              </Card>
            </Col>
          </Row>

          <Card size="small" style={{ marginTop: 16, overflow: 'hidden' }}>
            <div style={{ position: 'relative', height: 480 }}>
              <MapLibreMap
                center={mapCenter}
                zoom={14}
                height={480}
                markers={mapMarkers}
                lines={mapLines}
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
                <EnvironmentOutlined /> {mapCenter.lat.toFixed(4)}, {mapCenter.lng.toFixed(4)} · 共 {points.length} 个轨迹点
              </div>
            </div>
          </Card>
        </>
      )}
    </div>
  );
};

export default DeliveryTrajectoriesPage;
