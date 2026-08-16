import React, { useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Empty,
  Row,
  Space,
  Spin,
  Statistic,
  Typography,
  message,
} from 'antd';
import {
  AimOutlined,
  EnvironmentOutlined,
  ReloadOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { useQuery } from '@apollo/client';
import MapLibreMap from '../../../../../components/MapLibreMap';
import type { MapLine, MapMarker } from '../../../../../components/MapLibreMap';
import { DEFAULT_MAP_CENTER } from '../../../../../lib/map/tianditu';
import type { DeliveryTrajectory } from '@aquarush/common';
import dayjs from 'dayjs';
import {
  GET_DELIVERY_TRAJECTORIES_QUERY,
  GET_DELIVERY_WORKER_LATEST_LOCATION_QUERY,
} from '../../../../../graphql/queries/trajectory.graphql';

const { RangePicker } = DatePicker;

interface TrajectorySectionProps {
  /** 配送身份 id(送水员档案并入 admins 单表后=账号 id;F-USR-021/023) */
  workerId: string;
}

/**
 * 账号详情页「配送轨迹」Tab(F-ADR-012):
 * 展示该送水员/兼送水管理员在时间范围内的配送轨迹(deliveryTrajectories)与
 * 最近实时位置(deliveryWorkerLatestLocation),地图使用 MapLibre + 天地图底图。
 */
const TrajectorySection: React.FC<TrajectorySectionProps> = ({ workerId }) => {
  // 默认最近 7 天
  const [range, setRange] = useState<[string | null, string | null]>([
    dayjs().subtract(7, 'day').format('YYYY-MM-DDTHH:mm:ss'),
    dayjs().format('YYYY-MM-DDTHH:mm:ss'),
  ]);

  // 最近轨迹点(实时位置)
  const {
    data: latestData,
    loading: latestLoading,
    refetch: refetchLatest,
  } = useQuery<{ deliveryWorkerLatestLocation?: DeliveryTrajectory | null }>(
    GET_DELIVERY_WORKER_LATEST_LOCATION_QUERY,
    {
      variables: { workerId },
      skip: !workerId,
      fetchPolicy: 'cache-and-network',
    },
  );

  // 时间范围内轨迹序列(orderId 不指定=全部配送轨迹)
  const {
    data: trajData,
    refetch: refetchTrajectories,
  } = useQuery<{ deliveryTrajectories: readonly DeliveryTrajectory[] }>(GET_DELIVERY_TRAJECTORIES_QUERY, {
    variables: { workerId, orderId: null, startTime: range[0], endTime: range[1] },
    skip: !workerId,
    fetchPolicy: 'cache-and-network',
  });

  const points = useMemo(
    () =>
      (trajData?.deliveryTrajectories ?? []).map((t) => ({
        lat: t.latitude,
        lng: t.longitude,
        time: t.reportedAt,
        orderId: t.orderId,
      })),
    [trajData],
  );
  const latest = latestData?.deliveryWorkerLatestLocation;

  // 地图中心:优先最新位置,其次轨迹首点,兜底默认(轨迹坐标为 WGS-84,天地图直接展示)
  const mapCenter = useMemo(() => {
    if (latest) return { lng: latest.longitude, lat: latest.latitude };
    if (points.length > 0) return { lng: points[0].lng, lat: points[0].lat };
    return DEFAULT_MAP_CENTER;
  }, [latest, points]);

  const fullPath = useMemo(() => points.map((p) => ({ lng: p.lng, lat: p.lat })), [points]);

  // 地图折线图层:完整轨迹(蓝)
  const mapLines = useMemo<MapLine[]>(
    () => (points.length > 1 ? [{ id: 'full', points: fullPath, color: '#2F80ED', width: 6 }] : []),
    [points, fullPath],
  );

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

  const isDelivering = points.some((p) => p.orderId);

  const handleRefresh = async () => {
    try {
      await Promise.all([refetchLatest(), refetchTrajectories()]);
      message.success('位置与轨迹已刷新');
    } catch {
      message.error('获取轨迹数据失败');
    }
  };

  return (
    <Card title="配送轨迹">
      <Row gutter={16} style={{ marginBottom: 16 }} align="middle">
        <Col>
          <Space>
            <Typography.Text strong>时间范围:</Typography.Text>
            <RangePicker
              showTime
              allowClear
              value={[range[0] ? dayjs(range[0]) : null, range[1] ? dayjs(range[1]) : null]}
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
          <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
            刷新
          </Button>
        </Col>
      </Row>

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
                <Statistic
                  title="时间范围"
                  valueRender={() => (
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {range[0]?.slice(0, 10)} ~ {range[1]?.slice(0, 10)}
                    </Typography.Text>
                  )}
                />
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
          <Card size="small" title="说明">
            <Typography.Paragraph type="secondary" style={{ fontSize: 12, marginBottom: 0 }}>
              轨迹点来自配送中订单每 5s 上报(F-ADR-012),蓝色折线为选中时间范围内按订单关联的配送轨迹;无轨迹时展示空态。
            </Typography.Paragraph>
          </Card>
        </Col>
      </Row>

      <Card size="small" style={{ marginTop: 16, overflow: 'hidden' }}>
        {points.length === 0 && !latest ? (
          <Empty description="该账号暂无配送轨迹" style={{ padding: '48px 0' }} />
        ) : (
          <div style={{ position: 'relative', height: 420 }}>
            <MapLibreMap
              center={mapCenter}
              zoom={14}
              height={420}
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
        )}
      </Card>
    </Card>
  );
};

export default TrajectorySection;
