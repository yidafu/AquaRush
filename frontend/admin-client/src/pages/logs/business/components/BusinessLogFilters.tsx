import React from 'react';
import { Card, Space, Select, Input, DatePicker, Button, Segmented } from 'antd';
import { DOMAIN_OPTIONS, LOG_LEVEL_OPTIONS } from './constants';
import type { BizLogFilters } from './constants';

const { RangePicker } = DatePicker;
const { Option } = Select;

export interface BusinessLogFiltersProps {
  filters: BizLogFilters;
  operationOptions: { value: string; label: string }[];
  view: 'table' | 'timeline';
  onChange: (partial: Partial<BizLogFilters>) => void;
  onSearch: () => void;
  onReset: () => void;
  onViewChange: (view: 'table' | 'timeline') => void;
}

/** 业务日志筛选区(logs/business/index.tsx 拆分) */
const BusinessLogFilters: React.FC<BusinessLogFiltersProps> = ({
  filters,
  operationOptions,
  view,
  onChange,
  onSearch,
  onReset,
  onViewChange,
}) => (
  <Card style={{ marginBottom: 16 }}>
    <Space wrap>
      <Select
        placeholder="业务域"
        allowClear
        style={{ width: 120 }}
        value={filters.domain}
        onChange={(value) => {
          // 业务域变更后操作类型联动重置
          onChange({ domain: value, operationType: undefined });
        }}
      >
        {DOMAIN_OPTIONS.map(opt => (
          <Option key={opt.value} value={opt.value}>{opt.label}</Option>
        ))}
      </Select>
      <Select
        placeholder="操作类型"
        allowClear
        style={{ width: 150 }}
        value={filters.operationType}
        onChange={(value) => onChange({ operationType: value })}
      >
        {operationOptions.map(opt => (
          <Option key={opt.value} value={opt.value}>{opt.label}</Option>
        ))}
      </Select>
      <Select
        placeholder="日志级别"
        allowClear
        style={{ width: 120 }}
        value={filters.level}
        onChange={(value) => onChange({ level: value })}
      >
        {LOG_LEVEL_OPTIONS.map(opt => (
          <Option key={opt.value} value={opt.value}>{opt.label}</Option>
        ))}
      </Select>
      <Input
        placeholder="用户ID"
        allowClear
        style={{ width: 120 }}
        value={filters.userId}
        onChange={(e) => onChange({ userId: e.target.value ? Number(e.target.value) : undefined })}
      />
      <RangePicker
        showTime
        onChange={(dates) => {
          // 修复 G40:使用本地时间格式(弃用 toISOString 的 UTC 偏移)
          onChange({
            startTime: dates?.[0]?.format('YYYY-MM-DDTHH:mm:ss'),
            endTime: dates?.[1]?.format('YYYY-MM-DDTHH:mm:ss'),
          });
        }}
      />
      <Button type="primary" onClick={onSearch}>搜索</Button>
      <Button onClick={onReset}>重置</Button>
      <Segmented
        options={[
          { label: '表格', value: 'table' },
          { label: '时间线', value: 'timeline' },
        ]}
        value={view}
        onChange={(value) => onViewChange(value as 'table' | 'timeline')}
      />
    </Space>
  </Card>
);

export default BusinessLogFilters;
