import React from 'react';
import { Card, Row, Col, Space, Select, DatePicker, Button, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import * as stylex from '@stylexjs/stylex';
import type { DeliveryWorkerVo } from '@aquarush/common';

const { RangePicker } = DatePicker;

const styles = stylex.create({
  card: {
    marginBottom: '16px',
  },
  workerSelect: {
    width: '240px',
  },
});

export interface TrajectoryFiltersProps {
  workers: readonly DeliveryWorkerVo[];
  workersLoading: boolean;
  selectedWorkerId?: string;
  onWorkerChange: (value: string) => void;
  /** 时间范围变化(已格式化为后端要求的时间串) */
  onRangeChange: (range: [string | null, string | null]) => void;
  onRefresh: () => void;
  refreshDisabled: boolean;
}

/** 送水工选择 + 时间范围 + 刷新(trajectories/index.tsx 拆分) */
const TrajectoryFilters: React.FC<TrajectoryFiltersProps> = ({
  workers,
  workersLoading,
  selectedWorkerId,
  onWorkerChange,
  onRangeChange,
  onRefresh,
  refreshDisabled,
}) => (
  <Card {...stylex.props(styles.card)}>
    <Row gutter={16} align="middle">
      <Col>
        <Space>
          <Typography.Text strong>送水工:</Typography.Text>
          <Select
            placeholder="选择送水工查看轨迹"
            {...stylex.props(styles.workerSelect)}
            loading={workersLoading}
            value={selectedWorkerId}
            onChange={onWorkerChange}
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
              onRangeChange([
                dates?.[0] ? dates[0].format('YYYY-MM-DDTHH:mm:ss') : null,
                dates?.[1] ? dates[1].format('YYYY-MM-DDTHH:mm:ss') : null,
              ]);
            }}
          />
        </Space>
      </Col>
      <Col>
        <Button icon={<ReloadOutlined />} onClick={onRefresh} disabled={refreshDisabled}>
          刷新
        </Button>
      </Col>
    </Row>
  </Card>
);

export default TrajectoryFilters;
