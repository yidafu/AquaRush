import React from 'react';
import { Row, Col, Input, Select, DatePicker } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { ORDER_STATUS_TEXT } from '@aquarush/common';
import type { OrderStatus } from '@aquarush/common';
import FilterCard from '@/components/FilterCard';
import { useDebouncedFilters } from '@/hooks/useDebouncedFilters';

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

  const handleStatusChange = (status: string) => {
    setFilters(
      { ...localFilters, status: status === 'ALL' ? undefined : status },
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
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              关键词搜索
            </label>
            <Input
              placeholder="搜索订单号或用户名"
              value={localFilters.search || ''}
              onChange={handleSearchChange}
              allowClear
              prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
            />
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              订单状态
            </label>
            <Select
              style={{ width: '100%' }}
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
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              日期范围
            </label>
            <RangePicker
              style={{ width: '100%' }}
              onChange={handleDateRangeChange}
              placeholder={['开始日期', '结束日期']}
            />
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              金额范围（元）
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <Input
                type="number"
                style={{ flex: 1 }}
                placeholder="最低金额"
                value={localFilters.minAmount || ''}
                onChange={(e) => handleAmountRangeChange(0, e.target.value ? Number(e.target.value) * 100 : null)}
                min={0}
              />
              <Input
                type="number"
                style={{ flex: 1 }}
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
