import React from 'react';
import { Select, DatePicker, Space } from 'antd';
import type { Dayjs } from 'dayjs';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { BRAND_PRIMARY, COLOR_SUCCESS } from '../../../../config/theme';
import ChartCard from '@/components/ChartCard';

const { RangePicker } = DatePicker;

export interface SalesTrendChartProps {
  data: { date: string; sales: number; revenue: number }[];
  loading: boolean;
  productList: { id: number; name: string }[];
  selectedProductId: number | null;
  dateRange: [Dayjs, Dayjs];
  onProductChange: (id: number | null) => void;
  onDateRangeChange: (range: [Dayjs, Dayjs]) => void;
}

/** 日销量趋势折线图 + 商品选择/日期范围(statistics/products/index.tsx 拆分) */
const SalesTrendChart: React.FC<SalesTrendChartProps> = ({
  data,
  loading,
  productList,
  selectedProductId,
  dateRange,
  onProductChange,
  onDateRangeChange,
}) => (
  <ChartCard
    title="日销量趋势"
    loading={loading}
    height={350}
    style={{ marginTop: 24 }}
    extra={
      <Space>
        <Select
          placeholder="选择商品（可选）"
          allowClear
          style={{ width: 200 }}
          value={selectedProductId}
          onChange={onProductChange}
          options={productList.map((p) => ({
            value: p.id,
            label: p.name,
          }))}
        />
        <RangePicker
          value={dateRange}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              onDateRangeChange([dates[0], dates[1]]);
            }
          }}
        />
      </Space>
    }
  >
    <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
      <CartesianGrid strokeDasharray="3 3" />
      <XAxis dataKey="date" tick={{ fontSize: 12 }} />
      <YAxis yAxisId="left" tick={{ fontSize: 12 }} />
      <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} tickFormatter={(v) => `¥${v}`} />
      <Tooltip
        labelFormatter={(label) => `日期: ${label}`}
        formatter={(value, name) => {
          const numValue = typeof value === 'number' ? value : 0;
          const strName = typeof name === 'string' ? name : 'unknown';
          if (strName === 'sales') return [`${numValue} 桶`, '销量'];
          return [`¥${numValue.toFixed(2)}`, '营收'];
        }}
      />
      <Legend />
      <Line yAxisId="left" type="monotone" dataKey="sales" name="销量" stroke={BRAND_PRIMARY} strokeWidth={2} dot={{ fill: BRAND_PRIMARY, r: 3 }} />
      <Line yAxisId="right" type="monotone" dataKey="revenue" name="营收" stroke={COLOR_SUCCESS} strokeWidth={2} dot={{ fill: COLOR_SUCCESS, r: 3 }} />
    </LineChart>
  </ChartCard>
);

export default SalesTrendChart;
