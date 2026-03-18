import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Typography, Button, Space, message } from 'antd';
import { PlusOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useLazyQuery } from '@apollo/client';
import { ProductTable } from '../components/ProductTable';
import { ProductFilters } from '../components/ProductFilters';
import { ProductForm } from '../components/ProductForm';
import { GET_PRODUCTS_QUERY } from '../../../graphql/queries/product.graphql';
import { EnhancedWeChatPreview } from '../components/EnhancedWeChatPreview';

const ProductListPage: React.FC = () => {
  const navigate = useNavigate();
  const [createModalVisible, setCreateModalVisible] = useState(false);
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

  // Use lazy query to allow for filter variables
  const [loadProducts, { data, loading, refetch }] = useLazyQuery(GET_PRODUCTS_QUERY, {
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

  // Load products on mount and when filters change
  useEffect(() => {
    loadProducts();
  }, [filters]);

  const handleAddProduct = () => {
    setCreateModalVisible(true);
  };

  const handleViewDetail = (productId: number) => {
    navigate(`/products/${productId}`);
  };

  const handleFiltersChange = (newFilters: ProductFilters) => {
    setFilters(prev => ({ ...prev, ...newFilters }));
  };

  const handleCreateSuccess = () => {
    setCreateModalVisible(false);
    refetch();
    message.success('商品创建成功');
  };

  const handleModalCancel = () => {
    setCreateModalVisible(false);
  };

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div></div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddProduct}>
          新增商品
        </Button>
      </div>

      <ProductFilters
        filters={filters}
        onFiltersChange={handleFiltersChange}
      />
      <ProductTable
        loading={loading}
        data={data?.productsPaginated?.list || []}
        pagination={{
          current: (data?.productsPaginated?.pageInfo?.pageNum || 0) + 1,
          pageSize: data?.productsPaginated?.pageInfo?.pageSize || 20,
          total: data?.productsPaginated?.pageInfo?.total || 0,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`
        }}
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
      {/* Create Product Modal */}
      <ProductForm
        visible={createModalVisible}
        product={null}
        onCancel={handleModalCancel}
        onSuccess={handleCreateSuccess}
      />

    </div>
  );
};

export default ProductListPage;
