import React from 'react';
import { Col } from 'antd';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { BRAND_PRIMARY } from '../../../../config/theme';
import ChartCard from '@/components/ChartCard';

export interface ValueDistributionBarChartProps {
  data: { label: string; count: number }[];
  totalOrders: number;
  loading: boolean;
}

/** 客单价分布柱状图(statistics/orders/index.tsx 拆分) */
const ValueDistributionBarChart: React.FC<ValueDistributionBarChartProps> = ({ data, totalOrders, loading }) => (
  <Col xs={24} lg={8}>
    <ChartCard
      title="客单价分布"
      loading={loading}
      height={280}
      style={{ height: '100%' }}
      footer={<div style={{ textAlign: 'center', color: '#999', fontSize: 13 }}>区间合计 = 订单总数 ({totalOrders} 单)</div>}
    >
      <BarChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="label" tick={{ fontSize: 11 }} />
        <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
        <Tooltip
          formatter={(value) => {
            const numValue = typeof value === 'number' ? value : Number(value ?? 0);
            return [`${numValue} 单`, '订单数'];
          }}
        />
        <Bar dataKey="count" name="订单数" fill={BRAND_PRIMARY} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ChartCard>
  </Col>
);

export default ValueDistributionBarChart;
