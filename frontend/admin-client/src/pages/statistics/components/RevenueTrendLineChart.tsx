import React from 'react';
import { Card, Spin, Empty, Col } from 'antd';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { BRAND_PRIMARY } from '../../../config/theme';

export interface RevenueTrendLineChartProps {
  data: { date: string; revenue: number; netRevenue: number; refund: number }[];
  loading: boolean;
}

/** 营收/净营收/退款趋势折线图(statistics/index.tsx 拆分) */
const RevenueTrendLineChart: React.FC<RevenueTrendLineChartProps> = ({ data, loading }) => (
  <Col xs={24} lg={12}>
    <Card title="营收/净营收/退款趋势">
      <Spin spinning={loading}>
        {data.length === 0 ? (
          <Empty description="所选范围暂无数据" style={{ padding: 40 }} />
        ) : (
          <ResponsiveContainer width="100%" height={320}>
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
                stroke="#52c41a"
                strokeWidth={2}
                dot={{ fill: '#52c41a', r: 3 }}
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
                stroke="#ff4d4f"
                strokeWidth={2}
                dot={{ fill: '#ff4d4f', r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </Spin>
    </Card>
  </Col>
);

export default RevenueTrendLineChart;
