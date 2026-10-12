import React from 'react';
import { Select, DatePicker, Space } from '@aquarush/admin-ui';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import * as stylex from '@stylexjs/stylex';

const { RangePicker } = DatePicker;

// marginTop / marginBottom 依赖 props,保留 style={{ }} 动态注入
const styles = stylex.create({
  select: {
    width: '120px',
  },
});

/** 日期粒度(统计页共用;与 statistics/data.ts、statistics/orders/data.ts 的 Granularity 结构一致) */
export type StatisticsGranularity = 'DAY' | 'WEEK' | 'MONTH';

/** RangePicker 格式随粒度变化(与 statistics/data.ts getRangePickerFormat 一致) */
export const getStatisticsRangePickerFormat = (granularity: StatisticsGranularity): string => {
  switch (granularity) {
    case 'MONTH':
      return 'YYYY-MM';
    case 'WEEK':
      return 'YYYY-MM-DD';
    default:
      return 'YYYY-MM-DD';
  }
};

export interface StatisticsFiltersProps {
  granularity: StatisticsGranularity;
  dateRange: [Dayjs, Dayjs];
  onGranularityChange: (value: StatisticsGranularity) => void;
  onDateRangeChange: (range: [Dayjs, Dayjs]) => void;
  /** 顶部间距(默认 16,与旧 statistics 页一致) */
  marginTop?: number;
  /** 底部间距 */
  marginBottom?: number;
  /** 是否禁用未来日期(默认 true) */
  disableFuture?: boolean;
}

/**
 * 统计页日期粒度 + 时间范围筛选(statistics/index.tsx 与 statistics/orders/index.tsx 共用,
 * 原双胞胎 StatisticsFilters 合并于此)。
 */
const StatisticsFilters: React.FC<StatisticsFiltersProps> = ({
  granularity,
  dateRange,
  onGranularityChange,
  onDateRangeChange,
  marginTop = 16,
  marginBottom,
  disableFuture = true,
}) => (
  <Space style={{ marginTop, marginBottom }} wrap>
    <span>日期粒度:</span>
    <Select
      value={granularity}
      onChange={(value) => {
        // @aquarush/admin-ui Select onChange 泛型为 V | V[] | undefined;单选值收窄后回传
        if (typeof value === 'string') onGranularityChange(value as StatisticsGranularity);
      }}
      {...stylex.props(styles.select)}
    >
      <Select.Option value="DAY">按日</Select.Option>
      <Select.Option value="WEEK">按周</Select.Option>
      <Select.Option value="MONTH">按月</Select.Option>
    </Select>
    <RangePicker
      value={dateRange}
      format={getStatisticsRangePickerFormat(granularity)}
      disabledDate={(current) => (disableFuture ? current.isAfter(dayjs(), 'day') : false)}
      onChange={(dates) => {
        if (dates && dates[0] && dates[1]) {
          onDateRangeChange([dates[0], dates[1]]);
        }
      }}
    />
  </Space>
);

export default StatisticsFilters;
