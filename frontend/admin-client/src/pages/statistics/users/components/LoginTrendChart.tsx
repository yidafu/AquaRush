import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import ChartCard from '@/components/ChartCard';

export interface LoginTrendChartProps {
  data: { date: string; count: number }[];
  loading: boolean;
}

/** 登录趋势折线图(近30天,statistics/users/index.tsx 拆分) */
const LoginTrendChart: React.FC<LoginTrendChartProps> = ({ data, loading }) => (
  <ChartCard title="登录趋势（近30天）" loading={loading} height={300} style={{ marginTop: 24 }}>
    <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
      <CartesianGrid strokeDasharray="3 3" />
      <XAxis dataKey="date" tick={{ fontSize: 12 }} />
      <YAxis tick={{ fontSize: 12 }} />
      <Tooltip
        labelFormatter={(label) => `日期: ${label}`}
        formatter={(value) => [`${value ?? 0} 人`, '登录用户']}
      />
      <Line
        type="monotone"
        dataKey="count"
        name="登录用户"
        stroke="#52c41a"
        strokeWidth={2}
        dot={{ fill: '#52c41a', r: 3 }}
        activeDot={{ r: 6 }}
      />
    </LineChart>
  </ChartCard>
);

export default LoginTrendChart;
