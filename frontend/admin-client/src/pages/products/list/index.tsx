import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Typography, Button, Space, message } from 'antd';
import { PlusOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useLazyQuery } from '@apollo/client';
import { ProductTable } from '../components/ProductTable';
import { ProductFilters, ProductFilters as FilterType } from '../components/ProductFilters';
import { ProductForm } from '../components/ProductForm';
import { GET_PRODUCTS_QUERY } from '../../../graphql/queries/product.graphql';
import { EnhancedWeChatPreview } from '../components/EnhancedWeChatPreview';

const { Title } = Typography;

interface Product {
  id: number;
  name: string;
  subtitle?: string;
  price: number;
  originalPrice?: number;
  depositPrice?: number;
  coverImageUrl?: string;
  imageGallery?: string;
  specification: string;
  waterSource?: string;
  phValue?: number;
  mineralContent?: string;
  stock: number;
  salesVolume: number;
  status: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE';
  sortOrder: number;
  tags?: string;
  detailContent?: string;
  certificateImages?: string;
  deliverySettings?: string;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

const ProductListPage: React.FC = () => {
  const navigate = useNavigate();
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [filters, setFilters] = useState<FilterType>({
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
  });

  // Use lazy query to allow for filter variables
  const [loadProducts, { data, loading, refetch }] = useLazyQuery(GET_PRODUCTS_QUERY, {
    variables: {
      page: 0,
      size: 20,
      keyword: filters.keyword || undefined,
      status: filters.status !== 'ALL' ? filters.status : undefined
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

  const handleEditProduct = (product: Product) => {
    setEditingProduct(product);
    setEditModalVisible(true);
  };

  const handleQuickPreview = (productId: number) => {
    setSelectedProductId(productId);
  };

  const handleFiltersChange = (newFilters: FilterType) => {
    setFilters(prev => ({ ...prev, ...newFilters }));
  };

  const handleCreateSuccess = () => {
    setCreateModalVisible(false);
    refetch();
    message.success('商品创建成功');
  };

  const handleEditSuccess = () => {
    setEditModalVisible(false);
    setEditingProduct(null);
    refetch();
    message.success('商品更新成功');
  };

  const handleModalCancel = () => {
    setCreateModalVisible(false);
    setEditModalVisible(false);
    setEditingProduct(null);
  };

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <Title level={2} style={{ margin: 0 }}>商品管理</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddProduct}>
          新增商品
        </Button>
      </div>

      <Row gutter={24}>
        <Col xs={24} xl={selectedProductId ? 16 : 24}>
          <Card>
            <ProductFilters
              filters={filters}
              onFiltersChange={handleFiltersChange}
            />
            <ProductTable
              loading={loading}
              data={data?.products?.list || []}
              pagination={{
                current: (data?.products?.pageInfo?.pageNum || 0) + 1,
                pageSize: data?.products?.pageInfo?.pageSize || 20,
                total: data?.products?.pageInfo?.total || 0,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条记录`
              }}
              onProductSelect={handleQuickPreview}
              onProductEdit={handleEditProduct}
              onViewDetail={handleViewDetail}
              selectedProductId={selectedProductId}
              onChange={(pagination, filters, sorter) => {
                // Handle pagination and sorting
                console.log('Table change:', pagination, filters, sorter);
                refetch({
                  page: (pagination.current || 1) - 1,
                  size: pagination.pageSize || 20
                });
              }}
            />
          </Card>
        </Col>

        {selectedProductId && (
          <Col xs={24} xl={8}>
            <Card
              title={
                <Space>
                  <EyeOutlined />
                  快速预览
                  <Button
                    type="link"
                    size="small"
                    onClick={() => handleViewDetail(selectedProductId)}
                  >
                    查看详情
                  </Button>
                </Space>
              }
              style={{ position: 'relative', overflow: 'hidden' }}
              styles={{ body: { position: 'relative', zIndex: 2, padding: 0 } }}
              extra={
                <Button
                  type="text"
                  size="small"
                  onClick={() => setSelectedProductId(null)}
                >
                  ✕
                </Button>
              }
            >
              <EnhancedWeChatPreview
                productId={selectedProductId}
                style={{
                  height: '600px',
                  borderRadius: '0 0 8px 8px'
                }}
                showControls={true}
              />
            </Card>
          </Col>
        )}
      </Row>

      {/* Create Product Modal */}
      <ProductForm
        visible={createModalVisible}
        product={null}
        onCancel={handleModalCancel}
        onSuccess={handleCreateSuccess}
      />

      {/* Edit Product Modal */}
      <ProductForm
        visible={editModalVisible}
        product={editingProduct}
        onCancel={handleModalCancel}
        onSuccess={handleEditSuccess}
      />
    </div>
  );
};

export default ProductListPage;