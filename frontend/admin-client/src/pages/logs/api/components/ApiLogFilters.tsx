import React from 'react';
import { Card, Space, Select, Input, DatePicker, Button } from 'antd';
import type { ApiLogQueryParams } from '../../../../services/logs';

const { RangePicker } = DatePicker;
const { Option } = Select;

export interface ApiLogFiltersProps {
  filters: ApiLogQueryParams;
  onChange: (partial: Partial<ApiLogQueryParams>) => void;
  onSearch: () => void;
  onReset: () => void;
}

/** API 日志筛选区(logs/api/index.tsx 拆分) */
const ApiLogFilters: React.FC<ApiLogFiltersProps> = ({ filters, onChange, onSearch, onReset }) => (
  <Card style={{ marginBottom: 16 }}>
    <Space wrap>
      <Select
        placeholder="请求类型"
        allowClear
        style={{ width: 120 }}
        value={filters.requestType}
        onChange={(value) => onChange({ requestType: value })}
      >
        <Option value="HTTP">HTTP</Option>
        <Option value="GRAPHQL">GraphQL</Option>
      </Select>
      <Select
        placeholder="请求方法"
        allowClear
        style={{ width: 100 }}
        value={filters.method}
        onChange={(value) => onChange({ method: value })}
      >
        <Option value="GET">GET</Option>
        <Option value="POST">POST</Option>
        <Option value="PUT">PUT</Option>
        <Option value="DELETE">DELETE</Option>
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
          onChange({
            startTime: dates?.[0]?.toISOString(),
            endTime: dates?.[1]?.toISOString(),
          });
        }}
      />
      <Button type="primary" onClick={onSearch}>搜索</Button>
      <Button onClick={onReset}>重置</Button>
    </Space>
  </Card>
);

export default ApiLogFilters;
