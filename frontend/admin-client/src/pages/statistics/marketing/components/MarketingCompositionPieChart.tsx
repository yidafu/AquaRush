import React from 'react';
import { PieChart, Pie, Cell, Tooltip, Legend } from 'recharts';
import { centsToYuan } from '@aquarush/common';
import { BRAND_PRIMARY, COLOR_WARNING, COLOR_SUCCESS } from '../../../../config/theme';
import ChartCard from '@/components/ChartCard';

export interface CompositionSlice {
  name: string;
  value: number;
  color: string;
}

interface MarketingCompositionPieChartProps {
  data: CompositionSlice[];
  loading: boolean;
}

/**
 * 「营销方式构成」环形图(F-STA-007):区间让利金额按 券/满减/首单立减 占比。
 * 口径:订单快照列精确金额;赠品估算/水票抵扣不计入(PRD F-STA-007)。
 */
const MarketingCompositionPieChart: React.FC<MarketingCompositionPieChartProps> = ({ data, loading }) => {
  const total = data.reduce((sum, slice) => sum + slice.value, 0);
  return (
    <ChartCard
      title="营销方式构成"
      loading={loading}
      height={320}
      empty={total <= 0}
      emptyText="所选范围暂无让利数据"
      style={{ marginTop: 24 }}
    >
      <PieChart>
        <Pie
          data={data}
          dataKey="value"
          nameKey="name"
          cx="50%"
          cy="50%"
          innerRadius={60}
          outerRadius={100}
          paddingAngle={2}
        >
          {data.map((slice) => (
            <Cell key={slice.name} fill={slice.color} />
          ))}
        </Pie>
        <Tooltip formatter={(value) => `¥${Number(value ?? 0).toFixed(2)}`} />
        <Legend />
      </PieChart>
    </ChartCard>
  );
};

export const buildCompositionData = (params: {
  couponBenefitCents: number;
  promotionBenefitCents: number;
  firstOrderBenefitCents: number;
}): CompositionSlice[] => {
  const slices: CompositionSlice[] = [];
  if (params.couponBenefitCents > 0) {
    slices.push({ name: '券优惠', value: centsToYuan(params.couponBenefitCents), color: BRAND_PRIMARY });
  }
  if (params.promotionBenefitCents > 0) {
    slices.push({ name: '满减/折扣', value: centsToYuan(params.promotionBenefitCents), color: COLOR_WARNING });
  }
  if (params.firstOrderBenefitCents > 0) {
    slices.push({ name: '首单立减', value: centsToYuan(params.firstOrderBenefitCents), color: COLOR_SUCCESS });
  }
  return slices;
};

export default MarketingCompositionPieChart;
