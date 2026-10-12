import React from 'react';
import { Card, Col, Row, Statistic, Tag, Typography } from '@/ui';
import { SyncOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  hint: {
    fontSize: '12px',
  },
});

export interface TrajectoryStatsCardProps {
  pointsCount: number;
  workerOnline: boolean;
  isDelivering: boolean;
}

/** 轨迹统计卡(trajectories/index.tsx 拆分) */
const TrajectoryStatsCard: React.FC<TrajectoryStatsCardProps> = ({
  pointsCount,
  workerOnline,
  isDelivering,
}) => (
  <Col span={8}>
    <Card size="small" title="轨迹统计">
      <Row gutter={16}>
        <Col span={12}>
          <Statistic title="轨迹点数" value={pointsCount} suffix="个" />
        </Col>
        <Col span={12}>
          <Statistic
            title="送水工状态"
            valueRender={() => (
              <Tag color={workerOnline ? 'success' : 'default'}>
                {workerOnline ? '在线' : '离线'}
              </Tag>
            )}
          />
        </Col>
      </Row>
      {isDelivering ? (
        <Typography.Text type="secondary" {...stylex.props(styles.hint)}>
          <SyncOutlined spin /> 存在配送中订单,位置持续更新
        </Typography.Text>
      ) : null}
    </Card>
  </Col>
);

export default TrajectoryStatsCard;
