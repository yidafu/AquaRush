import React, { useState } from 'react';
import {
  Modal,
  Form,
  Input,
  InputNumber,
  Switch,
  Button,
  Select,
  Space,
  Divider,
  Collapse,
  Row,
  Col,
  message,
  AutoComplete
} from 'antd';
import { useCreateProduct, useUpdateProduct } from '../../../services/product-graphql';
import { SingleImageUpload, MultiImageUpload } from '../../../components/ImageUpload';
import { SimpleRichTextEditor } from '../../../components/RichTextEditor';
import { TagsEditor, DeliverySettingsEditor } from '../../../components/JsonEditor';

const { TextArea } = Input;
const { Panel } = Collapse;

// Predefined water sources for autocomplete
const waterSources = [
  '长白山',
  '昆仑山',
  '农夫山泉',
  '依云',
  '巴黎水',
  '圣培露',
  '斐济水',
  'VOSS',
  '西藏5100',
  '恒大冰泉'
];

// Predefined common specifications
const specifications = [
  '330ml',
  '500ml',
  '600ml',
  '750ml',
  '1L',
  '1.5L',
  '2L',
  '4L',
  '5L',
  '10L',
  '19L'
];

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
  isDeleted?: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ProductFormProps {
  visible: boolean;
  product?: Product | null;
  onCancel: () => void;
  onSuccess: () => void;
}

export const ProductForm: React.FC<ProductFormProps> = ({
  visible,
  product,
  onCancel,
  onSuccess
}) => {
  const [form] = Form.useForm();
  const [createProduct] = useCreateProduct();
  const [updateProduct] = useUpdateProduct();
  const [loading, setLoading] = useState(false);

  const isEdit = !!product;

  const handleSubmit = async () => {
    try {
      setLoading(true);
      const values = await form.validateFields();

      // Convert price values from yuan to cents
      const valuesInCents = {
        ...values,
        price: Math.round(values.price * 100),
        originalPrice: values.originalPrice ? Math.round(values.originalPrice * 100) : null,
        depositPrice: values.depositPrice ? Math.round(values.depositPrice * 100) : null,
        salesVolume: values.salesVolume || 0,
        sortOrder: values.sortOrder || 0,
      };

      // Parse JSON fields
      if (values.tags && typeof values.tags === 'string') {
        try {
          valuesInCents.tags = values.tags;
        } catch (e) {
          message.error('标签格式错误，请输入有效的JSON格式');
          return;
        }
      }

      if (values.deliverySettings && typeof values.deliverySettings === 'string') {
        try {
          valuesInCents.deliverySettings = values.deliverySettings;
        } catch (e) {
          message.error('配送设置格式错误，请输入有效的JSON格式');
          return;
        }
      }

      // Handle image gallery - if it's an array from upload component, convert to JSON
      if (values.imageGallery) {
        if (Array.isArray(values.imageGallery)) {
          valuesInCents.imageGallery = JSON.stringify(values.imageGallery);
        } else if (typeof values.imageGallery === 'string') {
          // Check if it's already a JSON string or just a single URL
          if (values.imageGallery.startsWith('[')) {
            valuesInCents.imageGallery = values.imageGallery;
          } else {
            // Single URL, wrap in array
            valuesInCents.imageGallery = JSON.stringify([values.imageGallery]);
          }
        }
      }

      // Handle certificate images - if it's an array from upload component, convert to JSON
      if (values.certificateImages) {
        if (Array.isArray(values.certificateImages)) {
          valuesInCents.certificateImages = JSON.stringify(values.certificateImages);
        } else if (typeof values.certificateImages === 'string') {
          // Check if it's already a JSON string or just a single URL
          if (values.certificateImages.startsWith('[')) {
            valuesInCents.certificateImages = values.certificateImages;
          } else {
            // Single URL, wrap in array
            valuesInCents.certificateImages = JSON.stringify([values.certificateImages]);
          }
        }
      }

      if (isEdit && product) {
        await updateProduct({
          variables: { id: product.id, input: valuesInCents }
        });
        message.success('更新成功');
      } else {
        await createProduct({ variables: { input: valuesInCents } });
        message.success('创建成功');
      }

      onSuccess?.();
      form.resetFields();
    } catch (error) {
      console.error('Form submission error:', error);
      message.error(isEdit ? '更新失败' : '创建失败');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  return (
    <Modal
      title={isEdit ? '编辑商品' : '新增商品'}
      open={visible}
      onCancel={handleCancel}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          取消
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={loading}
          onClick={handleSubmit}
        >
          {isEdit ? '更新' : '创建'}
        </Button>,
      ]}
      width={800}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          ...product,
          price: product ? product.price / 100 : undefined,
          originalPrice: product?.originalPrice ? product.originalPrice / 100 : undefined,
          depositPrice: product?.depositPrice ? product.depositPrice / 100 : undefined,
          status: product?.status || 'OFFLINE',
          salesVolume: product?.salesVolume || 0,
          sortOrder: product?.sortOrder || 0,
        }}
      >
        <Collapse defaultActiveKey={['basic', 'pricing', 'inventory']} ghost>
          {/* Basic Information */}
          <Panel header="基础信息" key="basic">
            <Row gutter={16}>
              <Col span={24}>
                <Form.Item
                  label="商品名称"
                  name="name"
                  rules={[
                    { required: true, message: '请输入商品名称' },
                    { min: 2, max: 200, message: '商品名称长度应在2-200个字符之间' }
                  ]}
                >
                  <Input placeholder="请输入商品名称" />
                </Form.Item>
              </Col>
              <Col span={24}>
                <Form.Item
                  label="商品副标题"
                  name="subtitle"
                  rules={[
                    { max: 500, message: '商品副标题长度不能超过500个字符' }
                  ]}
                >
                  <TextArea
                    rows={2}
                    placeholder="请输入商品副标题"
                    showCount
                    maxLength={500}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="规格"
                  name="specification"
                  rules={[
                    { required: true, message: '请输入商品规格' },
                    { min: 1, max: 100, message: '规格长度应在1-100个字符之间' }
                  ]}
                >
                  <AutoComplete
                    options={specifications.map(spec => ({ value: spec }))}
                    placeholder="如：500ml"
                    filterOption={(inputValue, option) =>
                      option!.value.toUpperCase().indexOf(inputValue.toUpperCase()) !== -1
                    }
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="水源地"
                  name="waterSource"
                  rules={[
                    { max: 200, message: '水源地长度不能超过200个字符' }
                  ]}
                >
                  <AutoComplete
                    options={waterSources.map(source => ({ value: source }))}
                    placeholder="如：长白山"
                    filterOption={(inputValue, option) =>
                      option!.value.indexOf(inputValue) !== -1
                    }
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="PH值"
                  name="phValue"
                  rules={[
                    { type: 'number', min: 0, max: 14, message: 'PH值应在0-14之间' }
                  ]}
                >
                  <InputNumber
                    min={0}
                    max={14}
                    step={0.1}
                    precision={1}
                    placeholder="7.0"
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="矿物质含量"
                  name="mineralContent"
                  rules={[
                    { max: 200, message: '矿物质含量长度不能超过200个字符' }
                  ]}
                >
                  <Input placeholder="如：钙 20mg/L, 镁 10mg/L" />
                </Form.Item>
              </Col>
            </Row>
          </Panel>

          {/* Pricing Information */}
          <Panel header="价格信息" key="pricing">
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="销售价格（元）"
                  name="price"
                  rules={[
                    { required: true, message: '请输入商品价格' },
                    { type: 'number', min: 0.01, max: 99999, message: '价格应在0.01-99999元之间' }
                  ]}
                >
                  <InputNumber
                    min={0.01}
                    max={99999}
                    precision={2}
                    placeholder="0.00"
                    style={{ width: '100%' }}
                    addonAfter="元"
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="原价（元）"
                  name="originalPrice"
                  rules={[
                    { type: 'number', min: 0.01, max: 99999, message: '原价应在0.01-99999元之间' }
                  ]}
                >
                  <InputNumber
                    min={0.01}
                    max={99999}
                    precision={2}
                    placeholder="0.00"
                    style={{ width: '100%' }}
                    addonAfter="元"
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="押金（元）"
                  name="depositPrice"
                  rules={[
                    { type: 'number', min: 0, max: 99999, message: '押金应在0-99999元之间' }
                  ]}
                >
                  <InputNumber
                    min={0}
                    max={99999}
                    precision={2}
                    placeholder="0.00"
                    style={{ width: '100%' }}
                    addonAfter="元"
                  />
                </Form.Item>
              </Col>
            </Row>
          </Panel>

          {/* Inventory & Sales */}
          <Panel header="库存与销售" key="inventory">
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="库存数量"
                  name="stock"
                  rules={[
                    { required: true, message: '请输入库存数量' },
                    { type: 'number', min: 0, max: 99999, message: '库存数量应在0-99999之间' }
                  ]}
                >
                  <InputNumber
                    min={0}
                    max={99999}
                    placeholder="0"
                    style={{ width: '100%' }}
                    addonAfter="件"
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="销量"
                  name="salesVolume"
                  rules={[
                    { type: 'number', min: 0, message: '销量不能小于0' }
                  ]}
                >
                  <InputNumber
                    min={0}
                    placeholder="0"
                    style={{ width: '100%' }}
                    addonAfter="件"
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="排序权重"
                  name="sortOrder"
                  rules={[
                    { required: true, message: '请输入排序权重' },
                    { type: 'number', min: 0, max: 9999, message: '排序权重应在0-9999之间' }
                  ]}
                >
                  <InputNumber
                    min={0}
                    max={9999}
                    placeholder="0"
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              </Col>
            </Row>
            <Row>
              <Col span={24}>
                <Form.Item
                  label="商品状态"
                  name="status"
                  rules={[{ required: true, message: '请选择商品状态' }]}
                >
                  <Select placeholder="请选择商品状态">
                    <Select.Option value="ONLINE">在线销售</Select.Option>
                    <Select.Option value="OFFLINE">下架</Select.Option>
                    <Select.Option value="OUT_OF_STOCK">缺货</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
            </Row>
          </Panel>

          {/* Product Details */}
          <Panel header="商品详情" key="details" forceRender>
            <Form.Item
              label="详情内容"
              name="detailContent"
              rules={[
                { max: 5000, message: '详情内容长度不能超过5000个字符' }
              ]}
            >
              <SimpleRichTextEditor
                placeholder="请输入商品详情内容，支持 Markdown 或 HTML 格式"
                maxLength={5000}
                showCount={true}
              />
            </Form.Item>
          </Panel>

          {/* Media */}
          <Panel header="媒体资源" key="media" forceRender>
            <Row gutter={16}>
              <Col span={24}>
                <Form.Item
                  label="封面图片"
                  name="coverImageUrl"
                  rules={[
                    { required: true, message: '请上传封面图片' },
                    { max: 500, message: 'URL长度不能超过500个字符' }
                  ]}
                >
                  <SingleImageUpload title="上传封面图片" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="图片画廊"
                  name="imageGallery"
                  help="上传商品展示图片，最多10张"
                  rules={[
                    { max: 2000, message: '图片URL长度不能超过2000个字符' }
                  ]}
                >
                  <MultiImageUpload maxCount={10} title="上传商品图片" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="证书图片"
                  name="certificateImages"
                  help="上传产品证书图片，最多5张"
                  rules={[
                    { max: 2000, message: '图片URL长度不能超过2000个字符' }
                  ]}
                >
                  <MultiImageUpload maxCount={5} title="上传证书图片" />
                </Form.Item>
              </Col>
            </Row>

            <Divider>图片URL手动输入（备用）</Divider>

            <Row gutter={16}>
              <Col span={24}>
                <Form.Item
                  label="封面图片URL（手动输入）"
                  name="coverImageUrl"
                  rules={[
                    { type: 'url', message: '请输入有效的URL地址', warningOnly: true }
                  ]}
                >
                  <Input placeholder="https://example.com/image.jpg" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="图片画廊JSON（手动输入）"
                  name="imageGallery"
                  help='格式：["url1", "url2", "url3"]'
                >
                  <TextArea
                    rows={3}
                    placeholder='["https://example.com/image1.jpg", "https://example.com/image2.jpg"]'
                    style={{ fontFamily: 'monospace' }}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="证书图片JSON（手动输入）"
                  name="certificateImages"
                  help='格式：["url1", "url2"]'
                >
                  <TextArea
                    rows={3}
                    placeholder='["https://example.com/cert1.jpg", "https://example.com/cert2.jpg"]'
                    style={{ fontFamily: 'monospace' }}
                  />
                </Form.Item>
              </Col>
            </Row>
          </Panel>

          {/* Settings */}
          <Panel header="设置" key="settings" forceRender>
            <Form.Item
              label="商品标签"
              name="tags"
              rules={[
                { max: 1000, message: '标签JSON长度不能超过1000个字符' }
              ]}
            >
              <TagsEditor
                maxLength={1000}
              />
            </Form.Item>

            <Form.Item
              label="配送设置"
              name="deliverySettings"
              rules={[
                { max: 2000, message: '配送设置JSON长度不能超过2000个字符' }
              ]}
            >
              <DeliverySettingsEditor
                maxLength={2000}
              />
            </Form.Item>
          </Panel>
        </Collapse>
      </Form>
    </Modal>
  );
};