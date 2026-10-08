import React from 'react';
import { Col } from 'antd';
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
} from 'recharts';
import { ORDER_STATUS_MAP } from '../data';
import ChartCard from '@/components/ChartCard';

export interface StatusPieChartProps {
  data: { name: string; status: string; value: number; color: string }[];
  totalOrders: number;
  loading: boolean;
}

/** 订单状态分布环形图(statistics/orders/index.tsx 拆分,G37) */
const StatusPieChart: React.FC<StatusPieChartProps> = ({ data, totalOrders, loading }) => (
  <Col xs={24} lg={8}>
    <ChartCard
      title="订单状态分布"
      loading={loading}
      height={280}
      style={{ height: '100%' }}
      footer={<div style={{ textAlign: 'center', color: '#999', fontSize: 13 }}>合计: {totalOrders} 单</div>}
    >
      <PieChart>
        <Pie
          data={data}
          dataKey="value"
          nameKey="name"
          cx="50%"
          cy="50%"
          innerRadius={60}
          outerRadius={95}
          paddingAngle={2}
        >
          {data.map((entry) => (
            <Cell key={entry.status} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip
          formatter={(value, name, item) => {
            const numValue = typeof value === 'number' ? value : Number(value ?? 0);
            const percent = totalOrders > 0 ? ((numValue / totalOrders) * 100).toFixed(1) : '0.0';
            const rawStatus = (item as unknown as { payload?: { status?: string } } | undefined)?.payload?.status;
            const label = rawStatus
              ? (ORDER_STATUS_MAP[rawStatus]?.text ?? rawStatus)
              : String(name ?? '');
            return [`${numValue} 单 (${percent}%)`, label];
          }}
        />
        <Legend />
      </PieChart>
    </ChartCard>
  </Col>
);

export default StatusPieChart;
