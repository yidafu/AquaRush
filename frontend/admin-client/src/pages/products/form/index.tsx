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
  Row,
  Col,
} from '@/ui';
import {
  ArrowLeftOutlined,
  EditOutlined,
  SaveOutlined,
  CloseOutlined,
  EyeOutlined,
} from '@/ui/icons';
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
import LiveProductPreview from './components/LiveProductPreview';
import { uniq, toProductInput } from './form-utils';
import { debounce } from '../../../utils/debounce';
import * as stylex from '@stylexjs/stylex';

// 原内联 padding: '24px' 单值拆 longhand
const styles = stylex.create({
  loadingBox: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
    textAlign: 'center',
  },
  page: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
  },
  title: {
    marginTop: '0px',
    marginRight: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
  },
  previewCard: {
    position: 'relative',
    overflow: 'hidden',
  },
  productIdText: {
    fontSize: '12px',
  },
});

const { Title, Text } = Typography;

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
      <div {...stylex.props(styles.loadingBox)}>
        <Spin size="large" tip="加载商品信息..." />
      </div>
    );
  }

  if (!isCreate && (error || !data?.product)) {
    return (
      <div {...stylex.props(styles.page)}>
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
    <div {...stylex.props(styles.page)}>
      {/* 页面头部:返回 + 标题 + 操作按钮 */}
      <div {...stylex.props(styles.header)}>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={handleBack}>
            返回列表
          </Button>
          <Title level={2} {...stylex.props(styles.title)}>
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
            {...stylex.props(styles.previewCard)}
            styles={{ body: { position: 'relative', zIndex: 2 } }}
            extra={
              !isCreate && product ? (
                <Text type="secondary" {...stylex.props(styles.productIdText)}>
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
