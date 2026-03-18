import React, { useState, useMemo } from 'react';
import { Card, Row, Col, Input, Select, Button, Space, InputNumber } from 'antd';
import { SearchOutlined, ClearOutlined } from '@ant-design/icons';

const { Option } = Select;

// 对应 GraphQL ProductSortBy 枚举
export type ProductSortByOption =
  | 'CREATED_AT_ASC'
  | 'CREATED_AT_DESC'
  | 'NAME_ASC'
  | 'NAME_DESC'
  | 'PRICE_ASC'
  | 'PRICE_DESC'
  | 'SALES_VOLUME_ASC'
  | 'SALES_VOLUME_DESC'
  | 'SORT_ORDER_ASC'
  | 'SORT_ORDER_DESC';

export interface ProductFilters {
  keyword?: string;
  search?: string;
  status?: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE' | 'ALL';
  // 价格范围（单位：分，转为元显示）
  minPrice?: number | null;
  maxPrice?: number | null;
  // 库存范围
  minStock?: number | null;
  maxStock?: number | null;
  // 销量范围
  minSalesVolume?: number | null;
  maxSalesVolume?: number | null;
  specification?: string;
  waterSource?: string;
  tags?: string;
  // 排序（对应 ProductSortBy 枚举）
  sortBy?: ProductSortByOption;
  // 分页
  page?: number;
  size?: number;
}

interface ProductFiltersProps {
  filters?: ProductFilters;
  onFiltersChange?: (filters: ProductFilters) => void;
}

export const ProductFilters: React.FC<ProductFiltersProps> = ({
  filters = {},
  onFiltersChange
}) => {
  const [localFilters, setLocalFilters] = useState<ProductFilters>(filters);

  const debouncedFilterChange = useMemo(() => {
    let timeoutId: number;
    return (newFilters: ProductFilters) => {
      clearTimeout(timeoutId);
      timeoutId = window.setTimeout(() => {
        onFiltersChange?.(newFilters);
      }, 300);
    };
  }, [onFiltersChange]);

  const handleKeywordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newFilters = { ...localFilters, keyword: e.target.value };
    setLocalFilters(newFilters);
    debouncedFilterChange(newFilters);
  };

  const handleStatusChange = (status: string) => {
    const newFilters = { ...localFilters, status: status as 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE' | 'ALL' };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleMinPriceChange = (value: number | null) => {
    const newFilters: ProductFilters = { ...localFilters, minPrice: value };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleMaxPriceChange = (value: number | null) => {
    const newFilters: ProductFilters = { ...localFilters, maxPrice: value };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleMinSalesVolumeChange = (value: number | null) => {
    const newFilters: ProductFilters = { ...localFilters, minSalesVolume: value };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleMaxSalesVolumeChange = (value: number | null) => {
    const newFilters: ProductFilters = { ...localFilters, maxSalesVolume: value };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleSortByChange = (value: ProductSortByOption) => {
    const newFilters: ProductFilters = { ...localFilters, sortBy: value };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleReset = () => {
    const emptyFilters: ProductFilters = {
      keyword: '',
      search: '',
      status: 'ALL',
      minPrice: null,
      maxPrice: null,
      minStock: null,
      maxStock: null,
      minSalesVolume: null,
      maxSalesVolume: null,
      specification: '',
      waterSource: '',
      tags: '',
      sortBy: 'CREATED_AT_DESC',
      page: 0,
      size: 20
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
              placeholder="搜索商品名称或描述"
              value={localFilters.keyword || ''}
              onChange={handleKeywordChange}
              allowClear
              prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
            />
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              商品状态
            </label>
            <Select
              style={{ width: '100%' }}
              value={localFilters.status || 'ALL'}
              onChange={handleStatusChange}
              placeholder="选择状态"
            >
              <Option value="ALL">全部</Option>
              <Option value="ONLINE">在售</Option>
              <Option value="OFFLINE">下架</Option>
              <Option value="OUT_OF_STOCK">缺货</Option>
            </Select>
          </div>
        </Col>


        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              价格范围（元）
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最低价"
                value={localFilters.minPrice || null}
                onChange={handleMinPriceChange}
                precision={2}
                min={0}
              />
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最高价"
                value={localFilters.maxPrice || null}
                onChange={handleMaxPriceChange}
                precision={2}
                min={0}
              />
            </div>
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              销量范围
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最低销量"
                value={localFilters.minSalesVolume || null}
                onChange={handleMinSalesVolumeChange}
                min={0}
              />
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最高销量"
                value={localFilters.maxSalesVolume || null}
                onChange={handleMaxSalesVolumeChange}
                min={0}
              />
            </div>
          </div>
        </Col>


        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              标签
            </label>
            <Input
              placeholder="搜索标签"
              value={localFilters.tags || ''}
              onChange={(e) => {
                const newFilters = { ...localFilters, tags: e.target.value };
                setLocalFilters(newFilters);
                onFiltersChange?.(newFilters);
              }}
              allowClear
            />
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              排序方式
            </label>
            <Select
              style={{ width: '100%' }}
              value={localFilters.sortBy || 'CREATED_AT_DESC'}
              onChange={handleSortByChange}
              placeholder="排序方式"
            >
              <Option value="CREATED_AT_DESC">创建时间 ↓</Option>
              <Option value="CREATED_AT_ASC">创建时间 ↑</Option>
              <Option value="SALES_VOLUME_DESC">销量 ↓</Option>
              <Option value="SALES_VOLUME_ASC">销量 ↑</Option>
              <Option value="PRICE_DESC">价格 ↓</Option>
              <Option value="PRICE_ASC">价格 ↑</Option>
              <Option value="SORT_ORDER_DESC">排序权重 ↓</Option>
              <Option value="SORT_ORDER_ASC">排序权重 ↑</Option>
              <Option value="NAME_ASC">名称 A-Z</Option>
              <Option value="NAME_DESC">名称 Z-A</Option>
            </Select>
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              库存范围
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最小库存"
                value={localFilters.minStock || null}
                onChange={(value) => {
                  const newFilters: ProductFilters = { ...localFilters, minStock: value };
                  setLocalFilters(newFilters);
                  onFiltersChange?.(newFilters);
                }}
                min={0}
              />
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最大库存"
                value={localFilters.maxStock || null}
                onChange={(value) => {
                  const newFilters: ProductFilters = { ...localFilters, maxStock: value };
                  setLocalFilters(newFilters);
                  onFiltersChange?.(newFilters);
                }}
                min={0}
              />
            </div>
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              快速操作
            </label>
            <Space>
              <Button size="small" disabled>导出数据</Button>
              <Button size="small" disabled>批量导入</Button>
            </Space>
          </div>
        </Col>
      </Row>
    </Card>
  );
};
