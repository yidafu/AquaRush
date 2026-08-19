import React from 'react';
import { Card, Spin, Empty, Col } from 'antd';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { BRAND_PRIMARY } from '../../../config/theme';

export interface OrderTrendBarChartProps {
  data: { date: string; orders: number }[];
  loading: boolean;
}

/** 订单趋势柱状图(statistics/index.tsx 拆分) */
const OrderTrendBarChart: React.FC<OrderTrendBarChartProps> = ({ data, loading }) => (
  <Col xs={24} lg={12}>
    <Card title="订单趋势">
      <Spin spinning={loading}>
        {data.length === 0 ? (
          <Empty description="所选范围暂无数据" style={{ padding: 40 }} />
        ) : (
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip
                labelFormatter={(label) => `日期: ${label}`}
                formatter={(value) => [`${Number(value)} 单`, '订单数']}
              />
              <Bar dataKey="orders" name="订单数" fill={BRAND_PRIMARY} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </Spin>
    </Card>
  </Col>
);

export default OrderTrendBarChart;
