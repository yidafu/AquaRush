import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  Form,
  Button,
  Space,
  Spin,
  Alert,
  Card,
  Typography,
  Tag,
  Empty,
  Row,
  Col,
} from 'antd';
import {
  ArrowLeftOutlined,
  EditOutlined,
  SaveOutlined,
  CloseOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { yuanToCents } from '@aquarush/common';
import {
  useCreateProduct,
  useUpdateProduct,
  useProductFormPageData,
} from '../../../services/product-graphql';
import { ProductFormFields } from '../components/ProductFormFields';
import { productToFormData } from '../components/hooks/useProductData';
import type { ProductFormData } from '../components/hooks/useProductData';
import { EnhancedWeChatPreview } from '../components/EnhancedWeChatPreview';
import { FloatingBubblingWall } from '../components/FloatingBubblingWall';
import { debounce } from '../../../utils/debounce';

const { Title, Text } = Typography;

/** 去重并保持先后顺序(配置项在前,当前值兜底补入) */
function uniq(values: string[]): string[] {
  return [...new Set(values.filter(Boolean))];
}

/**
 * 将表单值转换为 Create/UpdateProductInput 所需结构:
 * 价格由元转分;图片数组序列化为 JSON 字符串(与后端 JsonArray 标量兼容)。
 */
function toProductInput(values: ProductFormData): Record<string, unknown> {
  const input: Record<string, unknown> = {
    ...values,
    priceCents: yuanToCents(values.price),
    originalPriceCents: values.originalPrice ? yuanToCents(values.originalPrice) : null,
    salesVolume: values.salesVolume || 0,
    sortOrder: values.sortOrder || 0,
  };

  if (values.imageGallery) {
    const gallery = values.imageGallery as string[] | string;
    if (Array.isArray(gallery)) {
      input.imageGallery = JSON.stringify(gallery);
    } else if (typeof gallery === 'string') {
      input.imageGallery = gallery.startsWith('[')
        ? gallery
        : JSON.stringify([gallery]);
    }
  }

  if (values.certificateImages) {
    const certificates = values.certificateImages as string[] | string;
    if (Array.isArray(certificates)) {
      input.certificateImages = JSON.stringify(certificates);
    } else if (typeof certificates === 'string') {
      input.certificateImages = certificates.startsWith('[')
        ? certificates
        : JSON.stringify([certificates]);
    }
  }

  return input;
}

const statusConfig: Record<string, { text: string; color: string }> = {
  ONLINE: { text: '在售', color: 'green' },
  OFFLINE: { text: '下架', color: 'red' },
  OUT_OF_STOCK: { text: '缺货', color: 'orange' },
};

/**
 * 新增模式下的实时预览卡:展示左侧表单当前填写的商品信息(防抖更新)。
 */
const LiveProductPreview: React.FC<{ data: ProductFormData | null }> = ({ data }) => {
  if (!data || !data.name) {
    return (
      <div style={{ height: 640, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Empty description="填写左侧表单后，此处实时预览商品信息" />
      </div>
    );
  }

  const status = data.status ? statusConfig[data.status] : undefined;

  return (
    <div
      style={{
        width: 320,
        margin: '0 auto',
        background: '#fff',
        borderRadius: 16,
        border: '1px solid #e8e8e8',
        overflow: 'hidden',
        boxShadow: '0 4px 16px rgba(0,0,0,0.08)',
      }}
    >
      <div
        style={{
          height: 260,
          background: '#f0f2f5',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          overflow: 'hidden',
        }}
      >
        {data.coverImageUrl ? (
          <img
            src={data.coverImageUrl}
            alt="封面"
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        ) : (
          <Text type="secondary">暂无封面图</Text>
        )}
      </div>
      <div style={{ padding: 16 }}>
        <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 4 }}>{data.name}</div>
        {data.subtitle && (
          <Text type="secondary" style={{ fontSize: 13 }}>{data.subtitle}</Text>
        )}
        <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 20, fontWeight: 700, color: '#ff4d4f' }}>
            {data.price ? `¥${Number(data.price).toFixed(2)}` : '—'}
          </span>
          {data.specification && <Tag>{data.specification}</Tag>}
          {status && <Tag color={status.color}>{status.text}</Tag>}
        </div>
        <div style={{ marginTop: 8, fontSize: 13, color: '#999' }}>
          库存 {data.stock ?? 0} 件
        </div>
      </div>
    </div>
  );
};

/**
 * 商品统一页面(新增/编辑/详情,不弹框):
 * - /products/new       新增商品(表单 + 实时预览)
 * - /products/:id/edit  编辑商品(表单 + 小程序预览)
 * - /products/:id       商品详情(只读表单 + 小程序预览)
 * 左侧为商品表单,右侧为产品数据预览。
 */
const ProductFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();

  const isCreate = !id;
  const isEdit = !isCreate && location.pathname.endsWith('/edit');
  const isView = !isCreate && !isEdit;
  const editable = isCreate || isEdit;

  const [form] = Form.useForm();
  const [createProduct] = useCreateProduct();
  const [updateProduct] = useUpdateProduct();
  const [submitting, setSubmitting] = useState(false);
  const [livePreview, setLivePreview] = useState<ProductFormData | null>(null);

  // 详情/编辑模式:单页单请求拉取「商品数据 + 规格/水源地选项」(规则 #10);
  // 新增模式:单页单请求仅拉取选项;两种模式各只发一个 GraphQL 请求
  const { data, loading: dataLoading, error } = useProductFormPageData(id);

  const product = data?.product;

  // 下拉选项 = 后台配置字典(启用项) + 商品当前值兜底(存量值可能不在配置中,保证回显)
  const specificationOptions = useMemo(
    () => uniq([...(product?.specification ? [product.specification] : []), ...((data?.specificationOptions ?? []) as { value: string }[]).map((o) => o.value)]),
    [product, data]
  );
  const waterSourceOptions = useMemo(
    () => uniq([...(product?.waterSource ? [product.waterSource] : []), ...((data?.waterSourceOptions ?? []) as { value: string }[]).map((o) => o.value)]),
    [product, data]
  );
  const handleManageOptions = useCallback(() => {
    navigate('/products/options');
  }, [navigate]);

  useEffect(() => {
    if (data?.product) {
      form.setFieldsValue(productToFormData(data.product));
    }
  }, [data, form]);

  // 新增模式:表单输入防抖后实时刷新预览
  const debouncedSetLivePreview = useMemo(
    () => debounce((values: ProductFormData) => setLivePreview(values), 400),
    []
  );

  const handleValuesChange = useCallback((_: unknown, allValues: ProductFormData) => {
    if (isCreate) {
      debouncedSetLivePreview(allValues);
    }
  }, [isCreate, debouncedSetLivePreview]);

  const handleBack = () => {
    navigate('/products');
  };

  // 取消:新增回列表,编辑回该商品详情
  const handleCancel = () => {
    navigate(isCreate ? '/products' : `/products/${id}`);
  };

  const handleSubmit = async (values: ProductFormData) => {
    setSubmitting(true);
    try {
      const input = toProductInput(values);
      if (isCreate) {
        const result = await createProduct({ variables: { input } });
        const newId = result?.data?.createProduct?.id;
        navigate(newId ? `/products/${newId}` : '/products', { replace: true });
      } else {
        await updateProduct({ variables: { id, input } });
        navigate(`/products/${id}`, { replace: true });
      }
    } catch (e) {
      // 错误提示已由 mutation hook 的 onError 统一处理
      console.error('商品保存失败:', e);
    } finally {
      setSubmitting(false);
    }
  };

  if (!isCreate && dataLoading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Spin size="large" tip="加载商品信息..." />
      </div>
    );
  }

  if (!isCreate && (error || !data?.product)) {
    return (
      <div style={{ padding: '24px' }}>
        <Alert
          message="加载失败"
          description="商品信息加载失败，请稍后重试。"
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

  const pageTitle = isCreate ? '新增商品' : (isEdit ? '编辑商品' : product?.name || '商品详情');

  return (
    <div style={{ padding: '24px' }}>
      {/* 页面头部:返回 + 标题 + 操作按钮 */}
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
            {pageTitle}
          </Title>
        </Space>
        <Space>
          {isView && (
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={() => navigate(`/products/${id}/edit`)}
            >
              编辑商品
            </Button>
          )}
          {editable && (
            <>
              <Button
                icon={<CloseOutlined />}
                onClick={handleCancel}
              >
                取消
              </Button>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                loading={submitting}
                onClick={() => form.submit()}
              >
                保存
              </Button>
            </>
          )}
        </Space>
      </div>

      <Row gutter={24}>
        {/* 左侧:商品表单(详情模式只读,新增/编辑模式可编辑) */}
        <Col xs={24} lg={12}>
          <Card title={editable ? '商品信息' : '商品信息(只读)'}>
            <Form
              form={form}
              layout="vertical"
              disabled={!editable}
              onFinish={handleSubmit}
              onValuesChange={handleValuesChange}
              initialValues={
                isCreate
                  ? { status: 'OFFLINE', salesVolume: 0, sortOrder: 0 }
                  : product
                    ? productToFormData(product)
                    : undefined
              }
            >
              <ProductFormFields
                specificationOptions={specificationOptions}
                waterSourceOptions={waterSourceOptions}
                onManageOptions={handleManageOptions}
              />
            </Form>
          </Card>
        </Col>

        {/* 右侧:产品数据预览 */}
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <EyeOutlined />
                产品数据预览
              </Space>
            }
            style={{ position: 'relative', overflow: 'hidden' }}
            styles={{ body: { position: 'relative', zIndex: 2 } }}
            extra={
              !isCreate && product ? (
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  商品ID: {product.id}
                </Text>
              ) : undefined
            }
          >
            {isCreate ? (
              <LiveProductPreview data={livePreview} />
            ) : (
              <>
                <FloatingBubblingWall bubbleCount={8} />
                <EnhancedWeChatPreview
                  productId={product?.id || ''}
                  productName={product?.name}
                  height={700}
                />
              </>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ProductFormPage;
