import React from 'react';
import { Row, Col, Input, Select, DatePicker } from '@aquarush/admin-ui';
import { SearchOutlined } from '@aquarush/admin-ui/icons';
import { ORDER_STATUS_TEXT } from '@aquarush/common';
import type { OrderStatus } from '@aquarush/common';
import FilterCard from '@/components/FilterCard';
import { useDebouncedFilters } from '@/hooks/useDebouncedFilters';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  field: {
    marginBottom: '8px',
  },
  label: {
    fontSize: '14px',
    // 过滤项标签灰字 #666 为本组件局部色值,共享 token 无对应项
    color: '#666',
    marginBottom: '4px',
    display: 'block',
  },
  fullWidth: {
    width: '100%',
  },
  amountRow: {
    display: 'flex',
    columnGap: '8px',
  },
  amountInput: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: '0%',
  },
  searchPrefix: {
    // 搜索图标占位灰 #bfbfbf 为 antd 默认占位色,共享 token 无对应项
    color: '#bfbfbf',
  },
});

const { RangePicker } = DatePicker;
const { Option } = Select;

export interface OrderFilters {
  search?: string;
  status?: string;
  userId?: number;
  dateFrom?: string;
  dateTo?: string;
  minAmount?: number;
  maxAmount?: number;
  deliveryWorkerId?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

interface OrderFiltersProps {
  filters?: OrderFilters;
  onFiltersChange?: (filters: OrderFilters) => void;
}

// 订单状态下拉:文本统一来自 common ORDER_STATUS_TEXT(含 7 态,与后端 OrderStatus 枚举一致)
const STATUS_OPTIONS = (Object.keys(ORDER_STATUS_TEXT) as OrderStatus[]).map((value) => ({
  value,
  label: ORDER_STATUS_TEXT[value],
}));

export const OrderFilters: React.FC<OrderFiltersProps> = ({
  filters = {},
  onFiltersChange
}) => {
  // 本地状态 + 300ms 防抖(搜索字段防抖,状态/日期/金额即时);reset 回默认
  const { filters: localFilters, setFilters, reset } = useDebouncedFilters<OrderFilters>({
    initial: filters,
    onFiltersChange,
  });

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFilters({ ...localFilters, search: e.target.value });
  };

  const handleStatusChange = (status: string | string[] | undefined) => {
    const value = typeof status === 'string' ? status : undefined;
    setFilters(
      { ...localFilters, status: value === 'ALL' ? undefined : value },
      { immediate: true },
    );
  };

  const handleDateRangeChange = (_dates: unknown, dateStrings: [string, string]) => {
    setFilters(
      {
        ...localFilters,
        dateFrom: dateStrings[0] || undefined,
        dateTo: dateStrings[1] || undefined,
      },
      { immediate: true },
    );
  };

  const handleAmountRangeChange = (index: number, value: number | null) => {
    const newAmountRange = [localFilters.minAmount, localFilters.maxAmount] as [number | undefined, number | undefined];
    newAmountRange[index] = value || undefined;
    setFilters(
      {
        ...localFilters,
        minAmount: newAmountRange[0],
        maxAmount: newAmountRange[1],
      },
      { immediate: true },
    );
  };

  return (
    <FilterCard onReset={reset}>
      <Row gutter={16}>
        <Col xs={24} sm={12} md={6}>
          <div {...stylex.props(styles.field)}>
            <label {...stylex.props(styles.label)}>
              关键词搜索
            </label>
            <Input
              placeholder="搜索订单号或用户名"
              value={localFilters.search || ''}
              onChange={handleSearchChange}
              allowClear
              prefix={<SearchOutlined {...stylex.props(styles.searchPrefix)} />}
            />
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div {...stylex.props(styles.field)}>
            <label {...stylex.props(styles.label)}>
              订单状态
            </label>
            <Select
              {...stylex.props(styles.fullWidth)}
              value={localFilters.status || 'ALL'}
              onChange={handleStatusChange}
              placeholder="选择状态"
            >
              <Option value="ALL">全部</Option>
              {STATUS_OPTIONS.map((option) => (
                <Option key={option.value} value={option.value}>
                  {option.label}
                </Option>
              ))}
            </Select>
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div {...stylex.props(styles.field)}>
            <label {...stylex.props(styles.label)}>
              日期范围
            </label>
            <RangePicker
              {...stylex.props(styles.fullWidth)}
              onChange={handleDateRangeChange}
              placeholder={['开始日期', '结束日期']}
            />
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div {...stylex.props(styles.field)}>
            <label {...stylex.props(styles.label)}>
              金额范围（元）
            </label>
            <div {...stylex.props(styles.amountRow)}>
              <Input
                type="number"
                {...stylex.props(styles.amountInput)}
                placeholder="最低金额"
                value={localFilters.minAmount || ''}
                onChange={(e) => handleAmountRangeChange(0, e.target.value ? Number(e.target.value) * 100 : null)}
                min={0}
              />
              <Input
                type="number"
                {...stylex.props(styles.amountInput)}
                placeholder="最高金额"
                value={localFilters.maxAmount || ''}
                onChange={(e) => handleAmountRangeChange(1, e.target.value ? Number(e.target.value) * 100 : null)}
                min={0}
              />
            </div>
          </div>
        </Col>

      </Row>
    </FilterCard>
  );
};

export default OrderFilters;
