import React from 'react';
import { Select, DatePicker, Space } from 'antd';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { getRangePickerFormat } from '../data';
import type { Granularity } from '../data';

const { RangePicker } = DatePicker;

export interface StatisticsFiltersProps {
  granularity: Granularity;
  dateRange: [Dayjs, Dayjs];
  onGranularityChange: (value: Granularity) => void;
  onDateRangeChange: (range: [Dayjs, Dayjs]) => void;
}

/** 日期粒度 + 时间范围筛选(statistics/index.tsx 拆分) */
const StatisticsFilters: React.FC<StatisticsFiltersProps> = ({
  granularity,
  dateRange,
  onGranularityChange,
  onDateRangeChange,
}) => (
  <Space style={{ marginTop: 16 }} wrap>
    <span>日期粒度:</span>
    <Select value={granularity} onChange={onGranularityChange} style={{ width: 120 }}>
      <Select.Option value="DAY">按日</Select.Option>
      <Select.Option value="WEEK">按周</Select.Option>
      <Select.Option value="MONTH">按月</Select.Option>
    </Select>
    <RangePicker
      value={dateRange}
      format={getRangePickerFormat(granularity)}
      disabledDate={(current) => current.isAfter(dayjs(), 'day')}
      onChange={(dates) => {
        if (dates && dates[0] && dates[1]) {
          onDateRangeChange([dates[0], dates[1]]);
        }
      }}
    />
  </Space>
);

export default StatisticsFilters;
