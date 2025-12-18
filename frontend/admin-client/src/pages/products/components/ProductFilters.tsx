import React, { useState, useMemo } from 'react';
import { Card, Row, Col, Input, Select, Button, Space, InputNumber } from 'antd';
import { SearchOutlined, ClearOutlined } from '@ant-design/icons';

const { Option } = Select;

export interface ProductFilters {
  keyword?: string;
  status?: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE' | 'ALL';
  priceRange?: [number | null, number | null];
  stockLevel?: 'LOW' | 'NORMAL' | 'HIGH' | 'ALL';
  specification?: string;
  salesVolumeRange?: [number | null, number | null];
  waterSource?: string;
  tags?: string;
  sortBy?: 'createdAt' | 'salesVolume' | 'price' | 'sortOrder';
  sortOrder?: 'asc' | 'desc';
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

  const handleStockLevelChange = (stockLevel: string) => {
    const newFilters = { ...localFilters, stockLevel: stockLevel as 'LOW' | 'NORMAL' | 'HIGH' | 'ALL' };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handlePriceRangeChange = (index: number, value: number | null) => {
    const newPriceRange = [...(localFilters.priceRange || [null, null])] as [number | null, number | null];
    newPriceRange[index] = value;
    const newFilters = { ...localFilters, priceRange: newPriceRange };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleSalesVolumeRangeChange = (index: number, value: number | null) => {
    const newRange = [...(localFilters.salesVolumeRange || [null, null])] as [number | null, number | null];
    newRange[index] = value;
    const newFilters = { ...localFilters, salesVolumeRange: newRange };
    setLocalFilters(newFilters);
    onFiltersChange?.(newFilters);
  };

  const handleReset = () => {
    const emptyFilters: ProductFilters = {
      keyword: '',
      status: 'ALL',
      stockLevel: 'ALL',
      specification: '',
      priceRange: [null, null],
      salesVolumeRange: [null, null],
      waterSource: '',
      tags: '',
      sortBy: 'createdAt',
      sortOrder: 'desc'
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
              <Option value="ACTIVE">活跃</Option>
            </Select>
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              库存水平
            </label>
            <Select
              style={{ width: '100%' }}
              value={localFilters.stockLevel || 'ALL'}
              onChange={handleStockLevelChange}
              placeholder="选择库存水平"
            >
              <Option value="ALL">全部</Option>
              <Option value="LOW">低库存 (&lt;10)</Option>
              <Option value="NORMAL">正常 (10-100)</Option>
              <Option value="HIGH">高库存 (&gt;100)</Option>
            </Select>
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              规格
            </label>
            <Input
              placeholder="搜索规格"
              value={localFilters.specification || ''}
              onChange={(e) => {
                const newFilters = { ...localFilters, specification: e.target.value };
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
              价格范围（元）
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最低价"
                value={localFilters.priceRange?.[0] || null}
                onChange={(value) => handlePriceRangeChange(0, value)}
                precision={2}
                min={0}
              />
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最高价"
                value={localFilters.priceRange?.[1] || null}
                onChange={(value) => handlePriceRangeChange(1, value)}
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
                value={localFilters.salesVolumeRange?.[0] || null}
                onChange={(value) => handleSalesVolumeRangeChange(0, value)}
                min={0}
              />
              <InputNumber
                style={{ flex: 1 }}
                placeholder="最高销量"
                value={localFilters.salesVolumeRange?.[1] || null}
                onChange={(value) => handleSalesVolumeRangeChange(1, value)}
                min={0}
              />
            </div>
          </div>
        </Col>

        <Col xs={24} sm={12} md={6}>
          <div style={{ marginBottom: 8 }}>
            <label style={{ fontSize: '14px', color: '#666', marginBottom: '4px', display: 'block' }}>
              水源地
            </label>
            <Input
              placeholder="搜索水源地"
              value={localFilters.waterSource || ''}
              onChange={(e) => {
                const newFilters = { ...localFilters, waterSource: e.target.value };
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
            <div style={{ display: 'flex', gap: '8px' }}>
              <Select
                style={{ flex: 1 }}
                value={localFilters.sortBy || 'createdAt'}
                onChange={(value) => {
                  const newFilters = { ...localFilters, sortBy: value as any };
                  setLocalFilters(newFilters);
                  onFiltersChange?.(newFilters);
                }}
                placeholder="排序字段"
              >
                <Option value="createdAt">创建时间</Option>
                <Option value="salesVolume">销量</Option>
                <Option value="price">价格</Option>
                <Option value="sortOrder">排序权重</Option>
              </Select>
              <Select
                style={{ width: '100px' }}
                value={localFilters.sortOrder || 'desc'}
                onChange={(value) => {
                  const newFilters = { ...localFilters, sortOrder: value as any };
                  setLocalFilters(newFilters);
                  onFiltersChange?.(newFilters);
                }}
                placeholder="顺序"
              >
                <Option value="asc">升序</Option>
                <Option value="desc">降序</Option>
              </Select>
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