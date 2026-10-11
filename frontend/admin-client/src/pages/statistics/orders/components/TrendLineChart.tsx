import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_WARNING, COLOR_SUCCESS } from '../../../../config/theme';
import type { TrendChartPoint } from '../data';
import ChartCard from '@/components/ChartCard';

export interface TrendLineChartProps {
  data: TrendChartPoint[];
  loading: boolean;
}

/** 「订单与营收趋势」折线图(statistics/orders/index.tsx 拆分) */
const TrendLineChart: React.FC<TrendLineChartProps> = ({ data, loading }) => (
  <ChartCard title="订单与营收趋势" loading={loading} height={350} style={{ marginTop: 24 }}>
    <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
      <CartesianGrid strokeDasharray="3 3" />
      <XAxis dataKey="date" tick={{ fontSize: 12 }} />
      <YAxis yAxisId="left" tick={{ fontSize: 12 }} />
      <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} tickFormatter={(v) => `¥${v}`} />
      <Tooltip
        labelFormatter={(label) => `日期: ${label}`}
        formatter={(value, name) => {
          const numValue = typeof value === 'number' ? value : Number(value ?? 0);
          if (name === 'orders' || name === 'products') {
            return [`${numValue} 单`, name === 'orders' ? '订单数' : '送水桶数'];
          }
          return [`¥${numValue.toFixed(2)}`, '营收'];
        }}
      />
      <Legend />
      <Line yAxisId="left" type="monotone" dataKey="orders" name="订单数" stroke={BRAND_PRIMARY} strokeWidth={2} dot={{ fill: BRAND_PRIMARY, r: 3 }} />
      <Line yAxisId="left" type="monotone" dataKey="products" name="送水桶数" stroke={COLOR_WARNING} strokeWidth={2} dot={{ fill: COLOR_WARNING, r: 3 }} />
      <Line yAxisId="right" type="monotone" dataKey="revenue" name="营收" stroke={COLOR_SUCCESS} strokeWidth={2} dot={{ fill: COLOR_SUCCESS, r: 3 }} />
    </LineChart>
  </ChartCard>
);

export default TrendLineChart;
