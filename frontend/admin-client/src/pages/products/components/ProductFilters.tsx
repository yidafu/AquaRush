import React from 'react';
import { Row, Col, Input, Select, InputNumber, Space, Button } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import FilterCard from '@/components/FilterCard';
import { useDebouncedFilters } from '@/hooks/useDebouncedFilters';

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
  /** 卡片右上角自定义操作（与重置按钮同行） */
  extra?: React.ReactNode;
}

export const ProductFilters: React.FC<ProductFiltersProps> = ({
  filters = {},
  onFiltersChange,
  extra
}) => {
  // 本地状态 + 300ms 防抖(关键词防抖,状态/数值/排序即时);reset 回默认
  const { filters: localFilters, setFilters, reset } = useDebouncedFilters<ProductFilters>({
    initial: filters,
    onFiltersChange,
  });

  const handleKeywordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFilters({ ...localFilters, keyword: e.target.value });
  };

  const handleStatusChange = (status: string) => {
    setFilters(
      { ...localFilters, status: status as 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE' | 'ALL' },
      { immediate: true },
    );
  };

  const handleMinPriceChange = (value: number | null) => {
    setFilters({ ...localFilters, minPrice: value }, { immediate: true });
  };

  const handleMaxPriceChange = (value: number | null) => {
    setFilters({ ...localFilters, maxPrice: value }, { immediate: true });
  };

  const handleMinSalesVolumeChange = (value: number | null) => {
    setFilters({ ...localFilters, minSalesVolume: value }, { immediate: true });
  };

  const handleMaxSalesVolumeChange = (value: number | null) => {
    setFilters({ ...localFilters, maxSalesVolume: value }, { immediate: true });
  };

  const handleSortByChange = (value: ProductSortByOption) => {
    setFilters({ ...localFilters, sortBy: value }, { immediate: true });
  };

  return (
    <FilterCard onReset={reset} extra={extra}>
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
                setFilters({ ...localFilters, tags: e.target.value }, { immediate: true });
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
                  setFilters({ ...localFilters, minStock: value }, { immediate: true });
                }}
                min={0}
              />
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最大库存"
                value={localFilters.maxStock || null}
                onChange={(value) => {
                  setFilters({ ...localFilters, maxStock: value }, { immediate: true });
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
    </FilterCard>
  );
};

export default ProductFilters;
