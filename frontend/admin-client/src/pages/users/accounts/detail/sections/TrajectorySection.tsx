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
import { BaseMap, MultiMarker, MultiPolyline } from 'tlbs-map-react';
import type { DeliveryTrajectory } from '@aquarush/common';
import dayjs from 'dayjs';
import {
  GET_DELIVERY_TRAJECTORIES_QUERY,
  GET_DELIVERY_WORKER_LATEST_LOCATION_QUERY,
} from '../../../../../graphql/queries/trajectory.graphql';

const { RangePicker } = DatePicker;

// 腾讯地图 key(与配送轨迹页一致)
const TENCENT_MAP_KEY = '3YEBZ-G233L-GN7P7-M7EN6-YNLOH-VVBTT';

// 默认中心(无轨迹时的兜底位置)
const DEFAULT_CENTER = { lat: 39.9042, lng: 116.4074 };

/** 送水工当前位置标记样式 */
const workerMarkerStyles = {
  worker: {
    width: 28,
    height: 36,
    anchor: { x: 14, y: 36 },
    src: 'https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/markerDefault.png',
  },
  start: {
    width: 24,
    height: 24,
    anchor: { x: 12, y: 12 },
    src: 'https://mapapi.qq.com/web/lbs/javascriptGL/demo/img/start.png',
  },
};

/** 轨迹折线样式 */
const trajectoryStyles = {
  line: {
    color: '#2F80ED',
    width: 6,
    borderWidth: 2,
    borderColor: '#ffffff',
    lineCap: 'round' as const,
  },
};

interface TrajectorySectionProps {
  /** 配送身份 id(送水员档案并入 admins 单表后=账号 id;F-USR-021/023) */
  workerId: string;
}

/**
 * 账号详情页「配送轨迹」Tab(F-ADR-012):
 * 展示该送水员/兼送水管理员在时间范围内的配送轨迹(deliveryTrajectories)与
 * 最近实时位置(deliveryWorkerLatestLocation),地图复用 tlbs-map-react。
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

  // 地图中心:优先最新位置,其次轨迹首点,兜底默认
  const mapCenter = useMemo(() => {
    if (latest) return { lat: latest.latitude, lng: latest.longitude };
    if (points.length > 0) return { lat: points[0].lat, lng: points[0].lng };
    return DEFAULT_CENTER;
  }, [latest, points]);

  const fullPath = useMemo(() => points.map((p) => ({ lat: p.lat, lng: p.lng })), [points]);

  // 地图标记:当前位置 + 轨迹起点
  const markerGeometries = useMemo(() => {
    const geometries: Array<{ id: string; styleId: string; position: { lat: number; lng: number } }> = [];
    if (latest) {
      geometries.push({ id: 'worker', styleId: 'worker', position: { lat: latest.latitude, lng: latest.longitude } });
    } else if (points.length > 0) {
      // 非配送中无实时位置:以轨迹末点作为「最后位置」展示
      const last = points[points.length - 1];
      geometries.push({ id: 'worker', styleId: 'worker', position: { lat: last.lat, lng: last.lng } });
    }
    if (points.length > 1) {
      geometries.push({ id: 'start', styleId: 'start', position: { lat: points[0].lat, lng: points[0].lng } });
    }
    return geometries;
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
            <BaseMap apiKey={TENCENT_MAP_KEY} options={{ zoom: 14, center: mapCenter }}>
              {points.length > 1 ? (
                <MultiPolyline
                  styles={trajectoryStyles}
                  geometries={[{ id: 'full', styleId: 'line', paths: fullPath }]}
                />
              ) : null}
              <MultiMarker styles={workerMarkerStyles} geometries={markerGeometries} />
            </BaseMap>
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
