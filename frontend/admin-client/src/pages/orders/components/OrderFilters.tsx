import React, { useState, useMemo } from 'react';
import { Card, Row, Col, Input, Select, Button, Space, DatePicker } from 'antd';
import { SearchOutlined, ClearOutlined } from '@ant-design/icons';

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

export const OrderFilters: React.FC<OrderFiltersProps> = ({
  filters = {},
  onFiltersChange
}) => {
  const [localFilters, setLocalFilters] = useState<OrderFilters>(filters);

  const debouncedFilterChange = useMemo(() => {
    let timeoutId: number;
    return (newFilters: OrderFilters) => {
      clearTimeout(timeoutId);
      timeoutId = window.setTimeout(() => {
        onFiltersChange?.(newFilters);
      }, 300);
    };
  }, [onFiltersChange]);

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newFilters = { ...localFilters, search: e.target.value };
    setLocalFilters(newFilters);
    debouncedFilterChange(newFilters);
  };

  const handleStatusChange = (status: string) => {
    const newFilters = { ...localFilters, status: status === 'ALL' ? undefined : status };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleDateRangeChange = (dates: any, dateStrings: [string, string]) => {
    const newFilters = {
      ...localFilters,
      dateFrom: dateStrings[0] || undefined,
      dateTo: dateStrings[1] || undefined,
    };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleAmountRangeChange = (index: number, value: number | null) => {
    const newAmountRange = [localFilters.minAmount, localFilters.maxAmount] as [number | undefined, number | undefined];
    newAmountRange[index] = value || undefined;
    const newFilters = {
      ...localFilters,
      minAmount: newAmountRange[0],
      maxAmount: newAmountRange[1],
    };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleSortByChange = (value: string) => {
    const newFilters = { ...localFilters, sortBy: value };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleSortOrderChange = (value: 'asc' | 'desc') => {
    const newFilters = { ...localFilters, sortOrder: value };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleReset = () => {
    const emptyFilters: OrderFilters = {
      search: '',
      status: undefined,
      userId: undefined,
      dateFrom: undefined,
      dateTo: undefined,
      minAmount: undefined,
      maxAmount: undefined,
      deliveryWorkerId: undefined,
      sortBy: 'createdAt',
      sortOrder: 'desc',
    };
    setLocalFilters(emptyFilters);
    onFiltersChange?.(emptyFilters);
  };

  return (
    <Card
      size="small"
      style={{ marginBottom: 16 }}
      title="筛选条件"
      extra={
        <Button
          icon={<ClearOutlined />}
          size="small"
          onClick={handleReset}
        >
          重置
        </Button>
      }
    >
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
              <Option value="PENDING_PAYMENT">待支付</Option>
              <Option value="PENDING_DELIVERY">待配送</Option>
              <Option value="DELIVERING">配送中</Option>
              <Option value="COMPLETED">已完成</Option>
              <Option value="CANCELLED">已取消</Option>
              <Option value="REFUNDED">已退款</Option>
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
    </Card>
  );
};

export default OrderFilters;
