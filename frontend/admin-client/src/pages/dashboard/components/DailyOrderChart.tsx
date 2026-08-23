import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { BRAND_PRIMARY } from '../../../config/theme';
import ChartCard from '@/components/ChartCard';

interface DailyStatistic {
  date: string;
  orderCount: number;
  revenueCents: number;
}

interface DailyOrderChartProps {
  data: DailyStatistic[];
  title: string;
  loading: boolean;
  empty: boolean;
}

const DailyOrderChart: React.FC<DailyOrderChartProps> = ({ data, title, loading, empty }) => {
  const chartData = data.map((item) => ({
    date: item.date.slice(5),
    orderCount: item.orderCount || 0,
  }));

  return (
    <ChartCard title={title} loading={loading} empty={empty} emptyText="所选范围暂无数据" height={300}>
      <BarChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} />
        <Tooltip
          labelFormatter={(label) => `日期: ${label}`}
        />
        <Legend />
        <Bar
          dataKey="orderCount"
          name="订单数"
          fill={BRAND_PRIMARY}
          radius={[4, 4, 0, 0]}
        />
      </BarChart>
    </ChartCard>
  );
};

export default DailyOrderChart;
