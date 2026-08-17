import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { centsToYuan } from '@aquarush/common';
import { BRAND_PRIMARY } from '../../../config/theme';

interface DailyStatistic {
  date: string;
  orderCount: number;
  revenueCents: number;
}

interface DailyRevenueChartProps {
  data: DailyStatistic[];
}

const DailyRevenueChart: React.FC<DailyRevenueChartProps> = ({ data }) => {
  const chartData = data.map((item) => ({
    date: item.date.slice(5),
    revenue: centsToYuan(item.revenueCents || 0),
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" tick={{ fontSize: 12 }} />
        <YAxis
          tickFormatter={(value) => `¥${value}`}
          tick={{ fontSize: 12 }}
        />
        <Tooltip
          formatter={(value) => [`¥${Number(value).toFixed(2)}`, '营收']}
          labelFormatter={(label) => `日期: ${label}`}
        />
        <Line
          type="monotone"
          dataKey="revenue"
          stroke={BRAND_PRIMARY}
          strokeWidth={2}
          dot={{ fill: BRAND_PRIMARY, r: 4 }}
          activeDot={{ r: 6 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
};

export default DailyRevenueChart;