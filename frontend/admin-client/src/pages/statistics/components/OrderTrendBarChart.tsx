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
import { BRAND_PRIMARY } from '../../../config/theme';
import ChartCard from '@/components/ChartCard';

export interface OrderTrendBarChartProps {
  data: { date: string; orders: number }[];
  loading: boolean;
}

/** 订单趋势柱状图(statistics/index.tsx 拆分) */
const OrderTrendBarChart: React.FC<OrderTrendBarChartProps> = ({ data, loading }) => (
  <Col xs={24} lg={12}>
    <ChartCard
      title="订单趋势"
      loading={loading}
      height={320}
      empty={data.length === 0}
      emptyText="所选范围暂无数据"
    >
      <BarChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
        <Tooltip
          labelFormatter={(label) => `日期: ${label}`}
          formatter={(value) => [`${Number(value)} 单`, '订单数']}
        />
        <Bar dataKey="orders" name="订单数" fill={BRAND_PRIMARY} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ChartCard>
  </Col>
);

export default OrderTrendBarChart;
