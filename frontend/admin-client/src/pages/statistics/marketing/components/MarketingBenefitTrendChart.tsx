import React from 'react';
import { Card, Spin, Empty } from 'antd';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_WARNING, COLOR_SUCCESS } from '../../../../config/theme';

export interface MarketingBenefitPoint {
  date: string;
  coupon: number;
  promotion: number;
  firstOrder: number;
}

interface MarketingBenefitTrendChartProps {
  data: MarketingBenefitPoint[];
  loading: boolean;
}

/**
 * 「营销优惠趋势」折线图(F-STA-007):每日 券/满减/首单立减 优惠金额(元)。
 * 口径:订单快照列(coupon/promotion/first_order_discount_cents)按订单 createdAt 归属日。
 */
const MarketingBenefitTrendChart: React.FC<MarketingBenefitTrendChartProps> = ({ data, loading }) => {
  const hasData = data.some((p) => p.coupon > 0 || p.promotion > 0 || p.firstOrder > 0);
  return (
    <Card title="营销优惠趋势" style={{ marginTop: 24 }}>
      <Spin spinning={loading}>
        {hasData ? (
          <ResponsiveContainer width="100%" height={320}>
            <LineChart data={data} margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} tickFormatter={(v: number) => `¥${v}`} />
              <Tooltip
                labelFormatter={(label) => `日期: ${label}`}
                formatter={(value, name) => [`¥${Number(value ?? 0).toFixed(2)}`, String(name)]}
              />
              <Legend />
              <Line type="monotone" dataKey="coupon" name="券优惠" stroke={BRAND_PRIMARY} strokeWidth={2} dot={{ r: 2 }} />
              <Line type="monotone" dataKey="promotion" name="满减/折扣" stroke={COLOR_WARNING} strokeWidth={2} dot={{ r: 2 }} />
              <Line type="monotone" dataKey="firstOrder" name="首单立减" stroke={COLOR_SUCCESS} strokeWidth={2} dot={{ r: 2 }} />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <Empty description="所选范围暂无营销优惠数据" style={{ padding: '48px 0' }} />
        )}
      </Spin>
    </Card>
  );
};

export default MarketingBenefitTrendChart;
