import React, { useEffect, useMemo, useCallback } from 'react';
import {
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  Divider,
  Collapse,
  Row,
  Col,
  AutoComplete
} from 'antd';
import { SingleImageUpload, MultiImageUpload } from '../../../components/ImageUpload';
import { SimpleRichTextEditor } from '../../../components/RichTextEditor';
import { TagsEditor, DeliverySettingsEditor } from '../../../components/JsonEditor';
import { debounce } from '../../../utils/debounce';
import {
  productToFormData
} from './hooks/useProductData';
import type { Product, ProductFormData } from './hooks/useProductData';

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

interface ProductEditorProps {
  product: Product;
  editable?: boolean;
  form: any;
  onSave?: (values: ProductFormData) => Promise<void>;
  onPreviewUpdate?: (values: ProductFormData) => void;
  loading?: boolean;
}

export const ProductEditor: React.FC<ProductEditorProps> = ({
  product,
  editable = false,
  form,
  onSave,
  onPreviewUpdate,
  loading = false
}) => {
  const isReadonly = !editable;

  // Initialize form values when product or mode changes
  useEffect(() => {
    if (product && form) {
      const formData = productToFormData(product);
      form.setFieldsValue(formData);
    }
  }, [product, form, editable]);

  const debouncedPreviewUpdate = useMemo(
    () => debounce((values: ProductFormData) => {
      if (onPreviewUpdate) {
        onPreviewUpdate(values);
      }
    }, 500),
    [onPreviewUpdate]
  );

  const handleValuesChange = useCallback((_: any, allValues: ProductFormData) => {
    if (onPreviewUpdate) {
      debouncedPreviewUpdate(allValues);
    }
  }, [debouncedPreviewUpdate, onPreviewUpdate]);

  const handleFormSubmit = async () => {
    console.log('=== handleFormSubmit START ===');
    console.log('isReadonly:', isReadonly);
    console.log('onSave exists:', !!onSave);
    console.log('form exists:', !!form);

    if (isReadonly || !onSave) {
      console.log('Form submit skipped - readonly or no onSave handler');
      return;
    }

    try {
      console.log('Starting form validation...');
      const values = await form.validateFields();
      console.log('Form validation successful, values count:', Object.keys(values).length);
      console.log('Form values preview:', Object.keys(values));

      console.log('Calling onSave function...');
      await onSave(values);
      console.log('onSave completed successfully');
    } catch (error) {
      console.error('Form submission failed:', error);
      console.error('Error details:', error instanceof Error ? error.message : String(error));
    }
    console.log('=== handleFormSubmit END ===');
  };

  return (
    <Card title={editable ? "编辑商品信息" : "商品详情"} style={{ width: '100%' }}>
      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
        onFinish={handleFormSubmit}
        onError={console.error}
        disabled={!editable}
      >
        <Collapse
          defaultActiveKey={['basic', 'pricing', 'inventory']}
          ghost
          size="small"
        >
          {/* 基础信息 */}
          <Panel header="基础信息" key="basic">
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item label="商品ID" name="id">
                  <Input disabled />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="商品名称"
                  name="name"
                  rules={editable ? [
                    { required: true, message: '请输入商品名称' },
                    { min: 2, max: 200, message: '商品名称长度应在2-200个字符之间' }
                  ] : []}
                >
                  <Input placeholder="请输入商品名称" />
                </Form.Item>
              </Col>
              <Col span={24}>
                <Form.Item
                  label="商品副标题"
                  name="subtitle"
                  rules={editable ? [{ max: 500, message: '商品副标题长度不能超过500个字符' }] : []}
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
                  rules={editable ? [
                    { required: true, message: '请输入商品规格' },
                    { min: 1, max: 100, message: '规格长度应在1-100个字符之间' }
                  ] : []}
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
                  rules={editable ? [{ max: 200, message: '水源地长度不能超过200个字符' }] : []}
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
                  label="矿物质含量"
                  name="mineralContent"
                  rules={editable ? [{ max: 200, message: '矿物质含量长度不能超过200个字符' }] : []}
                >
                  <Input placeholder="如：钙 20mg/L, 镁 10mg/L" />
                </Form.Item>
              </Col>
            </Row>
          </Panel>

          {/* 价格信息 */}
          <Panel header="价格信息" key="pricing">
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="销售价格（元）"
                  name="price"
                  rules={editable ? [
                    { required: true, message: '请输入商品价格' },
                    { type: 'number', min: 0.01, max: 99999, message: '价格应在0.01-99999元之间' }
                  ] : []}
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
                  rules={editable ? [
                    { type: 'number', min: 0.01, max: 99999, message: '原价应在0.01-99999元之间' }
                  ] : []}
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
                  rules={editable ? [
                    { type: 'number', min: 0, max: 99999, message: '押金应在0-99999元之间' }
                  ] : []}
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

          {/* 库存与销售 */}
          <Panel header="库存与销售" key="inventory">
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="库存数量"
                  name="stock"
                  rules={editable ? [
                    { required: true, message: '请输入库存数量' },
                    { type: 'number', min: 0, max: 99999, message: '库存数量应在0-99999之间' }
                  ] : []}
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
                  rules={editable ? [
                    { type: 'number', min: 0, message: '销量不能小于0' }
                  ] : []}
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
                  rules={editable ? [
                    { required: true, message: '请输入排序权重' },
                    { type: 'number', min: 0, max: 9999, message: '排序权重应在0-9999之间' }
                  ] : []}
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
                  rules={editable ? [{ required: true, message: '请选择商品状态' }] : []}
                >
                  <Select placeholder="请选择商品状态" disabled={!editable}>
                    <Select.Option value="ONLINE">在线销售</Select.Option>
                    <Select.Option value="OFFLINE">下架</Select.Option>
                    <Select.Option value="OUT_OF_STOCK">缺货</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
            </Row>
          </Panel>

          {/* 商品详情 */}
          <Panel header="商品详情" key="details" forceRender>
            <Form.Item
              label="详情内容"
              name="detailContent"
              rules={editable ? [
                { max: 5000, message: '详情内容长度不能超过5000个字符' }
              ] : []}
            >
              <SimpleRichTextEditor
                placeholder="请输入商品详情内容，支持 Markdown 或 HTML 格式"
                maxLength={5000}
                showCount={true}
              />
            </Form.Item>
          </Panel>

          {/* 媒体资源 */}
          <Panel header="媒体资源" key="media" forceRender>
            <Row gutter={16}>
              <Col span={24}>
                <Form.Item
                  label="封面图片"
                  name="coverImageUrl"
                  rules={editable ? [
                    { required: true, message: '请上传封面图片' },
                    { max: 500, message: 'URL长度不能超过500个字符' }
                  ] : []}
                >
                  <SingleImageUpload />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="图片画廊"
                  name="imageGallery"
                  help="上传商品展示图片，最多10张"
                >
                  <MultiImageUpload maxCount={10} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="证书图片"
                  name="certificateImages"
                  help="上传产品证书图片，最多5张"
                >
                  <MultiImageUpload maxCount={5} />
                </Form.Item>
              </Col>
            </Row>

            <Divider>图片URL手动输入（备用）</Divider>

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

          {/* 设置 */}
          <Panel header="设置" key="settings" forceRender>
            <Form.Item
              label="商品标签"
              name="tags"
            >
              <TagsEditor />
            </Form.Item>

            <Form.Item
              label="配送设置"
              name="deliverySettings"
            >
              <DeliverySettingsEditor />
            </Form.Item>
          </Panel>
        </Collapse>
      </Form>
    </Card>
  );
};
