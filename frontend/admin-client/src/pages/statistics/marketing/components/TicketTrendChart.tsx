import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_ERROR } from '../../../../config/theme';
import ChartCard from '@/components/ChartCard';

export interface TicketTrendPoint {
  date: string;
  sales: number;
  redeemed: number;
}

interface TicketTrendChartProps {
  data: TicketTrendPoint[];
  loading: boolean;
}

/**
 * 「水票销售与核销趋势」折线图(F-STA-007):每日 销售张数/核销张数。
 * 口径:销售 = ticket_purchases PAID 按 paidAt 归属(张数含赠送 N+M);核销 = user_tickets USED 按 usedAt。
 */
const TicketTrendChart: React.FC<TicketTrendChartProps> = ({ data, loading }) => {
  const hasData = data.some((p) => p.sales > 0 || p.redeemed > 0);
  return (
    <ChartCard
      title="水票销售与核销趋势"
      loading={loading}
      height={320}
      empty={!hasData}
      emptyText="所选范围暂无水票数据"
      style={{ marginTop: 24 }}
    >
      <LineChart data={data} margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} />
        <Tooltip
          labelFormatter={(label) => `日期: ${label}`}
          formatter={(value, name) => [`${Number(value ?? 0)} 张`, String(name)]}
        />
        <Legend />
        <Line type="monotone" dataKey="sales" name="销售张数" stroke={BRAND_PRIMARY} strokeWidth={2} dot={{ r: 2 }} />
        <Line type="monotone" dataKey="redeemed" name="核销张数" stroke={COLOR_ERROR} strokeWidth={2} dot={{ r: 2 }} />
      </LineChart>
    </ChartCard>
  );
};

export default TicketTrendChart;
