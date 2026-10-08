import React from 'react';
import { Col } from 'antd';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import ChartCard from '@/components/ChartCard';

export interface FavoriteRankingChartProps {
  data: { name: string; favorites: number }[];
  loading: boolean;
}

/** 商品收藏排行 TOP10 柱状图(statistics/products/index.tsx 拆分) */
const FavoriteRankingChart: React.FC<FavoriteRankingChartProps> = ({ data, loading }) => (
  <Col xs={24} lg={12}>
    <ChartCard title="商品收藏排行 TOP10" loading={loading} height={350}>
      <BarChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" tick={{ fontSize: 10 }} angle={-45} textAnchor="end" height={80} />
        <YAxis tick={{ fontSize: 12 }} />
        <Tooltip
          labelFormatter={(label) => `商品: ${label}`}
          formatter={(value) => [`${Number(value)} 人`, '收藏数']}
        />
        <Legend />
        <Bar dataKey="favorites" name="收藏数" fill="#fa8c16" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ChartCard>
  </Col>
);

export default FavoriteRankingChart;
