import React, { useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Row, Col, Typography, Button, Space, Spin, Alert, Form, Modal, message, Card } from 'antd';
import { ArrowLeftOutlined, EditOutlined, SaveOutlined, CloseOutlined, EyeOutlined } from '@ant-design/icons';
import { useProductDetail, useUpdateProduct } from '../../../services/product-graphql';
import { EnhancedWeChatPreview } from '../components/EnhancedWeChatPreview';
import { FloatingBubblingWall } from '../components/FloatingBubblingWall';
import { ProductEditor } from '../components/ProductEditor';
import type { ProductFormData } from '../components/hooks/useProductData';
import { formDataToProduct } from '../components/hooks/useProductData';

const { Title, Text } = Typography;

interface Product {
  id: number;
  name: string;
  subtitle?: string;
  price: number;
  originalPrice?: number;
  depositPrice?: number;
  coverImageUrl?: string;
  imageGallery?: string | string[];
  specification: string;
  waterSource?: string;
  mineralContent?: string;
  stock: number;
  salesVolume: number;
  status: 'ONLINE' | 'OFFLINE' | 'OUT_OF_STOCK' | 'ACTIVE';
  sortOrder: number;
  tags?: string | string[];
  detailContent?: string;
  certificateImages?: string | string[];
  deliverySettings?: any;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
  description?: string;
}

type ViewMode = 'readonly' | 'edit';

const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [viewMode, setViewMode] = useState<ViewMode>('readonly');
  const [loading, setLoading] = useState(false);
  const [previewData, setPreviewData] = useState<Product | null>(null);
  const originalDataRef = useRef<Product | null>(null);

  const { data, loading: dataLoading, error, refetch } = useProductDetail(id!);
  const [updateProduct] = useUpdateProduct();

  const handleBack = () => {
    navigate('/products');
  };

  const handleEdit = () => {
    if (data?.product) {
      originalDataRef.current = data.product;
      setPreviewData(data.product);
      setViewMode('edit');
    }
  };

  const handleSave = async (values: ProductFormData) => {
    console.log('handleSave called with values:', values);
    console.log('data.product exists:', !!data?.product);
    if (!data?.product) return;

    try {
      setLoading(true);

      // Convert form data to product format
      const productData = formDataToProduct(values);

      // Handle image gallery and certificate images arrays - send as arrays directly
      // The ArrayNodeConverter on the backend will handle the conversion
      if (Array.isArray(values.imageGallery)) {
        productData.imageGallery = values.imageGallery;
      }

      if (Array.isArray(values.certificateImages)) {
        productData.certificateImages = values.certificateImages;
      }

      console.log('Saving product with data:', productData);

      // Call update mutation
      const result = await updateProduct({
        variables: {
          id: data.product.id,
          input: productData
        }
      });

      console.log('Update result:', result);

      message.success('商品更新成功');
      setViewMode('readonly');

      // Refetch data to get latest updates
      refetch();
    } catch (error) {
      console.error('Update failed:', error);
      message.error(`更新失败：${error instanceof Error ? error.message : '请重试'}`);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    // Check if there are unsaved changes
    if (originalDataRef.current && previewData) {
      Modal.confirm({
        title: '确认取消编辑？',
        content: '您有未保存的更改，取消编辑将丢失这些更改。',
        okText: '确认取消',
        cancelText: '继续编辑',
        onOk: () => {
          setViewMode('readonly');
          setPreviewData(null);
          form.resetFields();
        }
      });
    } else {
      setViewMode('readonly');
      setPreviewData(null);
      form.resetFields();
    }
  };

  const handlePreviewUpdate = (values: ProductFormData) => {
    if (originalDataRef.current) {
      const productData = formDataToProduct(values);
      const updatedPreview = {
        ...originalDataRef.current,
        ...productData,
        // Handle array fields
        imageGallery: Array.isArray(values.imageGallery)
          ? JSON.stringify(values.imageGallery)
          : originalDataRef.current.imageGallery,
        certificateImages: Array.isArray(values.certificateImages)
          ? JSON.stringify(values.certificateImages)
          : originalDataRef.current.certificateImages,
      };
      setPreviewData(updatedPreview);
    }
  };

  if (dataLoading || loading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Spin size="large" tip={loading ? "保存中..." : "加载商品详情..."} />
      </div>
    );
  }

  if (error || !data?.product) {
    return (
      <div style={{ padding: '24px' }}>
        <Alert
          message="加载失败"
          description="商品详情加载失败，请稍后重试。"
          type="error"
          action={
            <Space>
              <Button onClick={handleBack}>返回列表</Button>
              <Button type="primary" onClick={() => window.location.reload()}>
                重新加载
              </Button>
            </Space>
          }
        />
      </div>
    );
  }

  const product = data.product;
  const displayProduct = viewMode === 'edit' && previewData ? previewData : product;

  return (
    <div style={{ padding: '24px' }}>
      {/* 页面头部 */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '24px'
      }}>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={handleBack}>
            返回列表
          </Button>
          <Title level={2} style={{ margin: 0 }}>
            {product.name}
          </Title>
        </Space>
        <Space>
          {viewMode === 'readonly' ? (
            <Button type="primary" icon={<EditOutlined />} onClick={handleEdit}>
              编辑商品
            </Button>
          ) : (
            <>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                loading={loading}
                onClick={() => {
                  console.log('Save button clicked - triggering form.submit()');
                  form.submit();
                }}
              >
                保存
              </Button>
              <Button
                icon={<CloseOutlined />}
                onClick={handleCancel}
              >
                取消
              </Button>
            </>
          )}
        </Space>
      </div>

      <Row gutter={24}>
        {/* 左侧：商品信息展示或编辑 */}
        <Col xs={24} lg={12}>
          <ProductEditor
            product={product}
            mode={viewMode}
            form={form}
            onSave={handleSave}
            onPreviewUpdate={handlePreviewUpdate}
            loading={loading}
          />
        </Col>

        {/* 右侧：小程序预览 */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <EyeOutlined />
                小程序预览
              </Space>
            }
            style={{ position: 'relative', overflow: 'hidden' }}
            styles={{ body: { position: 'relative', zIndex: 2 } }}
            extra={
              <Space>
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  商品ID: {displayProduct.id}
                </Text>
              </Space>
            }
          >
            <FloatingBubblingWall bubbleCount={8} />
            <EnhancedWeChatPreview
              productId={displayProduct.id}
              productName={displayProduct.name}
              height={700}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ProductDetailPage;
