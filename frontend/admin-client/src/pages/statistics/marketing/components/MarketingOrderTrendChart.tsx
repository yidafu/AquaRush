import React from 'react';
import { Card, Spin, Empty } from 'antd';
import {
  ComposedChart,
  Bar,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_WARNING } from '../../../../config/theme';

export interface MarketingOrderPoint {
  date: string;
  orders: number;
  coupons: number;
}

interface MarketingOrderTrendChartProps {
  data: MarketingOrderPoint[];
  loading: boolean;
}

/**
 * 「营销订单与用券趋势」组合图(F-STA-007):每日营销订单数柱状 + 用券数折线。
 */
const MarketingOrderTrendChart: React.FC<MarketingOrderTrendChartProps> = ({ data, loading }) => {
  const hasData = data.some((p) => p.orders > 0 || p.coupons > 0);
  return (
    <Card title="营销订单与用券趋势" style={{ marginTop: 24 }}>
      <Spin spinning={loading}>
        {hasData ? (
          <ResponsiveContainer width="100%" height={320}>
            <ComposedChart data={data} margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip
                labelFormatter={(label) => `日期: ${label}`}
                formatter={(value, name) => [`${Number(value ?? 0)} ${name === 'orders' ? '单' : '张'}`, String(name)]}
              />
              <Legend />
              <Bar dataKey="orders" name="营销订单数" fill={BRAND_PRIMARY} radius={[4, 4, 0, 0]} />
              <Line type="monotone" dataKey="coupons" name="用券数" stroke={COLOR_WARNING} strokeWidth={2} dot={{ r: 2 }} />
            </ComposedChart>
          </ResponsiveContainer>
        ) : (
          <Empty description="所选范围暂无营销订单数据" style={{ padding: '48px 0' }} />
        )}
      </Spin>
    </Card>
  );
};

export default MarketingOrderTrendChart;
