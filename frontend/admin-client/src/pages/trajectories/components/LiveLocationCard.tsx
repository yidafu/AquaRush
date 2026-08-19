import React from 'react';
import { Card, Col, Spin, Descriptions, Typography } from 'antd';
import { AimOutlined } from '@ant-design/icons';
import type { DeliveryTrajectory } from '@aquarush/common';

export interface LiveLocationCardProps {
  latest: DeliveryTrajectory | null | undefined;
  loading: boolean;
}

/** 实时位置信息卡(trajectories/index.tsx 拆分) */
const LiveLocationCard: React.FC<LiveLocationCardProps> = ({ latest, loading }) => (
  <Col span={8}>
    <Card size="small" title="实时位置">
      <Spin spinning={loading}>
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
);

export default LiveLocationCard;
