import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { BRAND_PRIMARY } from '../../../../config/theme';
import ChartCard from '@/components/ChartCard';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  chartCard: {
    marginTop: '24px',
  },
});

export interface UserGrowthChartProps {
  data: { date: string; count: number }[];
  loading: boolean;
}

/** 用户增长趋势折线图(近30天,statistics/users/index.tsx 拆分) */
const UserGrowthChart: React.FC<UserGrowthChartProps> = ({ data, loading }) => (
  <ChartCard title="用户增长趋势（近30天）" loading={loading} height={300} {...stylex.props(styles.chartCard)}>
    <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
      <CartesianGrid strokeDasharray="3 3" />
      <XAxis dataKey="date" tick={{ fontSize: 12 }} />
      <YAxis tick={{ fontSize: 12 }} />
      <Tooltip
        labelFormatter={(label) => `日期: ${label}`}
        formatter={(value) => [`${value ?? 0} 人`, '新增用户']}
      />
      <Line
        type="monotone"
        dataKey="count"
        name="新增用户"
        stroke={BRAND_PRIMARY}
        strokeWidth={2}
        dot={{ fill: BRAND_PRIMARY, r: 3 }}
        activeDot={{ r: 6 }}
      />
    </LineChart>
  </ChartCard>
);

export default UserGrowthChart;
