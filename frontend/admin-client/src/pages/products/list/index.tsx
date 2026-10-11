import React, { useState } from 'react';
import { Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@apollo/client';
import { ProductTable } from '../components/ProductTable';
import { ProductFilters } from '../components/ProductFilters';
import { PRODUCTS_PAGE_DATA_QUERY } from '../../../graphql/queries/product.graphql';
import { pagination } from '@/utils/table';
import * as stylex from '@stylexjs/stylex';

// 原内联 padding: '24px' 单值拆 longhand
const styles = stylex.create({
  page: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
  },
});

const ProductListPage: React.FC = () => {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<ProductFilters>({
    keyword: '',
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
  });

  // 单页单请求:商品分页 + 库存预警阈值(着色)一次取回(规则 #10);
  // 筛选/分页变化经 variables 变化自动重取
  const { data, loading } = useQuery(PRODUCTS_PAGE_DATA_QUERY, {
    variables: {
      input: {
        page: filters.page || 0,
        size: filters.size || 20,
        keyword: filters.keyword || undefined,
        search: filters.search || undefined,
        status: filters.status !== 'ALL' ? filters.status : undefined,
        minPrice: filters.minPrice || undefined,
        maxPrice: filters.maxPrice || undefined,
        minStock: filters.minStock || undefined,
        maxStock: filters.maxStock || undefined,
        minSalesVolume: filters.minSalesVolume || undefined,
        maxSalesVolume: filters.maxSalesVolume || undefined,
        sortBy: filters.sortBy || undefined
      }
    },
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all'
  });

  // 新增商品:跳转独立的新增页(不弹框)
  const handleAddProduct = () => {
    navigate('/products/new');
  };

  const handleViewDetail = (productId: number) => {
    navigate(`/products/${productId}`);
  };

  const handleFiltersChange = (newFilters: ProductFilters) => {
    setFilters(prev => ({ ...prev, ...newFilters }));
  };

  return (
    <div {...stylex.props(styles.page)}>
      <ProductFilters
        filters={filters}
        onFiltersChange={handleFiltersChange}
        extra={
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={handleAddProduct}>
            新增商品
          </Button>
        }
      />
      <ProductTable
        loading={loading}
        data={data?.productsPaginated?.list || []}
        stockThresholds={
          data?.stockWarningThresholds
            ? { low: data.stockWarningThresholds.lowThreshold, mid: data.stockWarningThresholds.midThreshold }
            : { low: 10, mid: 50 }
        }
        pagination={pagination(data?.productsPaginated?.pageInfo?.total || 0, {
          current: (data?.productsPaginated?.pageInfo?.pageNum || 0) + 1,
          pageSize: data?.productsPaginated?.pageInfo?.pageSize || 20,
          showSizeChanger: true,
          showQuickJumper: true,
        })}
        onViewDetail={handleViewDetail}
        onChange={(pagination) => {
          // Handle pagination and sorting
          setFilters(prev => ({
            ...prev,
            page: (pagination.current || 1) - 1,
            size: pagination.pageSize || 20
          }));
        }}
      />
    </div>
  );
};

export default ProductListPage;
