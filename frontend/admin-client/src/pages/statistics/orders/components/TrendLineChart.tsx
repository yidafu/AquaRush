import React from 'react';
import { Card, Spin } from 'antd';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_WARNING } from '../../../../config/theme';
import type { TrendChartPoint } from '../data';

export interface TrendLineChartProps {
  data: TrendChartPoint[];
  loading: boolean;
}

/** 「订单与营收趋势」折线图(statistics/orders/index.tsx 拆分) */
const TrendLineChart: React.FC<TrendLineChartProps> = ({ data, loading }) => (
  <Card title="订单与营收趋势" style={{ marginTop: 24 }}>
    <Spin spinning={loading}>
      <ResponsiveContainer width="100%" height={350}>
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
          <Line yAxisId="right" type="monotone" dataKey="revenue" name="营收" stroke="#52c41a" strokeWidth={2} dot={{ fill: '#52c41a', r: 3 }} />
        </LineChart>
      </ResponsiveContainer>
    </Spin>
  </Card>
);

export default TrendLineChart;
