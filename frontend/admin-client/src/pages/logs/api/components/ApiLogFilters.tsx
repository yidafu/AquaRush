import React from 'react';
import { Card, Space, Select, Input, DatePicker, Button } from '@aquarush/admin-ui';
import * as stylex from '@stylexjs/stylex';
import type { ApiLogQueryParams } from '../../../../services/logs';

const { RangePicker } = DatePicker;
const { Option } = Select;

const styles = stylex.create({
  filterCard: {
    marginBottom: '16px',
  },
  width120: {
    width: '120px',
  },
  width100: {
    width: '100px',
  },
});

export interface ApiLogFiltersProps {
  filters: ApiLogQueryParams;
  onChange: (partial: Partial<ApiLogQueryParams>) => void;
  onSearch: () => void;
  onReset: () => void;
}

/** API 日志筛选区(logs/api/index.tsx 拆分) */
const ApiLogFilters: React.FC<ApiLogFiltersProps> = ({ filters, onChange, onSearch, onReset }) => (
  <Card {...stylex.props(styles.filterCard)}>
    <Space wrap>
      <Select
        placeholder="请求类型"
        allowClear
        {...stylex.props(styles.width120)}
        value={filters.requestType}
        onChange={(value: unknown) => onChange({ requestType: value as 'HTTP' | 'GRAPHQL' | undefined })}
      >
        <Option value="HTTP">HTTP</Option>
        <Option value="GRAPHQL">GraphQL</Option>
      </Select>
      <Select
        placeholder="请求方法"
        allowClear
        {...stylex.props(styles.width100)}
        value={filters.method}
        onChange={(value: unknown) => onChange({ method: value as string | undefined })}
      >
        <Option value="GET">GET</Option>
        <Option value="POST">POST</Option>
        <Option value="PUT">PUT</Option>
        <Option value="DELETE">DELETE</Option>
      </Select>
      <Input
        placeholder="用户ID"
        allowClear
        {...stylex.props(styles.width120)}
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
