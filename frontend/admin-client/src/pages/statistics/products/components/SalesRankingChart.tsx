import React from 'react';
import { Col } from '@aquarush/admin-ui';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_SUCCESS } from '../../../../config/theme';
import ChartCard from '@/components/ChartCard';

export interface SalesRankingChartProps {
  data: { name: string; sales: number; revenue: number }[];
  loading: boolean;
}

/** 商品销量排行 TOP10 柱状图(statistics/products/index.tsx 拆分) */
const SalesRankingChart: React.FC<SalesRankingChartProps> = ({ data, loading }) => (
  <Col xs={24} lg={12}>
    <ChartCard title="商品销量排行 TOP10" loading={loading} height={350}>
      <BarChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" tick={{ fontSize: 10 }} angle={-45} textAnchor="end" height={80} />
        <YAxis yAxisId="left" tick={{ fontSize: 12 }} />
        <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} tickFormatter={(v) => `¥${v}`} />
        <Tooltip
          labelFormatter={(label) => `商品: ${label}`}
          formatter={(value, name) => {
            if (name === 'sales') return [`${Number(value)} 桶`, '销量'];
            return [`¥${Number(value).toFixed(2)}`, '营收'];
          }}
        />
        <Legend />
        <Bar yAxisId="left" dataKey="sales" name="销量" fill={BRAND_PRIMARY} radius={[4, 4, 0, 0]} />
        <Bar yAxisId="right" dataKey="revenue" name="营收" fill={COLOR_SUCCESS} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ChartCard>
  </Col>
);

export default SalesRankingChart;
