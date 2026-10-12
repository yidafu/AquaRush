import React from 'react';
import { Col } from '@aquarush/admin-ui';
import {
  BarChart,
  Bar,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_WARNING } from '../../../../config/theme';
import ChartCard from '@/components/ChartCard';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  chartCard: {
    height: '100%',
  },
});

export interface PeakHourBarChartProps {
  data: { hour: string; count: number }[];
  maxHourCount: number;
  peakHourExtra: string | null;
  loading: boolean;
}

/** 高峰时段分析柱状图(statistics/orders/index.tsx 拆分,24 小时,标注高峰小时) */
const PeakHourBarChart: React.FC<PeakHourBarChartProps> = ({
  data,
  maxHourCount,
  peakHourExtra,
  loading,
}) => (
  <Col xs={24} lg={8}>
    <ChartCard
      title="高峰时段分析"
      extra={peakHourExtra}
      loading={loading}
      height={280}
      {...stylex.props(styles.chartCard)}
    >
      <BarChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="hour" tick={{ fontSize: 10 }} interval={1} />
        <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
        <Tooltip
          formatter={(value) => {
            const numValue = typeof value === 'number' ? value : Number(value ?? 0);
            return [`${numValue} 单`, '订单数'];
          }}
        />
        <Bar dataKey="count" name="订单数">
          {data.map((entry) => (
            <Cell
              key={entry.hour}
              fill={entry.count === maxHourCount && maxHourCount > 0 ? COLOR_WARNING : BRAND_PRIMARY}
            />
          ))}
        </Bar>
      </BarChart>
    </ChartCard>
  </Col>
);

export default PeakHourBarChart;
