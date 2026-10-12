import React from 'react';
import { Col } from '@aquarush/admin-ui';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_ERROR, COLOR_SUCCESS } from '../../../config/theme';
import ChartCard from '@/components/ChartCard';

export interface RevenueTrendLineChartProps {
  data: { date: string; revenue: number; netRevenue: number; refund: number }[];
  loading: boolean;
}

/** 营收/净营收/退款趋势折线图(statistics/index.tsx 拆分) */
const RevenueTrendLineChart: React.FC<RevenueTrendLineChartProps> = ({ data, loading }) => (
  <Col xs={24} lg={12}>
    <ChartCard
      title="营收/净营收/退款趋势"
      loading={loading}
      height={320}
      empty={data.length === 0}
      emptyText="所选范围暂无数据"
    >
      <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `¥${v}`} />
        <Tooltip
          labelFormatter={(label) => `日期: ${label}`}
          formatter={(value, name) => [`¥${Number(value).toFixed(2)}`, name]}
        />
        <Line
          type="monotone"
          dataKey="revenue"
          name="营收"
          stroke={COLOR_SUCCESS}
          strokeWidth={2}
          dot={{ fill: COLOR_SUCCESS, r: 3 }}
        />
        <Line
          type="monotone"
          dataKey="netRevenue"
          name="净营收"
          stroke={BRAND_PRIMARY}
          strokeWidth={2}
          strokeDasharray="6 4"
          dot={{ fill: BRAND_PRIMARY, r: 3 }}
        />
        <Line
          type="monotone"
          dataKey="refund"
          name="退款"
          stroke={COLOR_ERROR}
          strokeWidth={2}
          dot={{ fill: COLOR_ERROR, r: 3 }}
        />
      </LineChart>
    </ChartCard>
  </Col>
);

export default RevenueTrendLineChart;
