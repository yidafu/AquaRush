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
  AutoComplete,
  Typography,
  Image,
  Tag
} from 'antd';
import { SingleImageUpload, MultiImageUpload } from '../../../components/ImageUpload';
import { SimpleRichTextEditor } from '../../../components/RichTextEditor';
import { TagsEditor, DeliverySettingsEditor } from '../../../components/JsonEditor';
import { debounce } from '../../../utils/debounce';
import {
  productToFormData,
  useFormattedPrices,
  useParsedJSONFields,
  useFormattedStatus,
  useStockStatus
} from './hooks/useProductData';
import type { Product, ProductFormData } from './hooks/useProductData';

const { TextArea } = Input;
const { Panel } = Collapse;
const { Text } = Typography;

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
  mode: 'readonly' | 'edit';
  form: any;
  onSave?: (values: ProductFormData) => Promise<void>;
  onPreviewUpdate?: (values: ProductFormData) => void;
  loading?: boolean;
}

export const ProductEditor: React.FC<ProductEditorProps> = ({
  product,
  mode,
  form,
  onSave,
  onPreviewUpdate,
  loading = false
}) => {
  const isReadonly = mode === 'readonly';
  const formattedPrices = useFormattedPrices(product);
  const parsedFields = useParsedJSONFields(product);
  const statusFormat = useFormattedStatus(product);
  const stockStatus = useStockStatus(product);

  // Initialize form values when product or mode changes
  useEffect(() => {
    if (product && form) {
      const formData = productToFormData(product);
      form.setFieldsValue(formData);
    }
  }, [product, form, mode]);

  const debouncedPreviewUpdate = useMemo(
    () => debounce((values: ProductFormData) => {
      if (onPreviewUpdate && !isReadonly) {
        onPreviewUpdate(values);
      }
    }, 500),
    [onPreviewUpdate, isReadonly]
  );

  const handleValuesChange = useCallback((_: any, allValues: ProductFormData) => {
    if (!isReadonly) {
      debouncedPreviewUpdate(allValues);
    }
  }, [debouncedPreviewUpdate, isReadonly]);

  const handleFormSubmit = async () => {
    if (isReadonly || !onSave) return;

    try {
      const values = await form.validateFields();
      await onSave(values);
    } catch (error) {
      console.error('Form validation failed:', error);
    }
  };

  // 只读模式下的渲染函数
  const renderReadonlyField = (label: string, value: React.ReactNode) => (
    <div style={{ marginBottom: '16px' }}>
      <Text strong>{label}：</Text>
      <div style={{ marginTop: '4px' }}>{value}</div>
    </div>
  );

  const renderImageGallery = () => {
    const { imageGallery } = parsedFields;
    if (!imageGallery.length) return null;

    return (
      <div style={{ marginTop: '16px' }}>
        <Text strong>图片画廊：</Text>
        <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
          {imageGallery.map((url: string, index: number) => (
            <Image
              key={index}
              src={url}
              alt={`商品图片${index + 1}`}
              width={100}
              height={100}
              style={{
                objectFit: 'cover',
                borderRadius: '4px',
                border: '1px solid #d9d9d9'
              }}
            />
          ))}
        </div>
      </div>
    );
  };

  const renderCertificateImages = () => {
    const { certificateImages } = parsedFields;
    if (!certificateImages.length) return null;

    return (
      <div style={{ marginTop: '16px' }}>
        <Text strong>证书图片：</Text>
        <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
          {certificateImages.map((url: string, index: number) => (
            <Image
              key={index}
              src={url}
              alt={`证书图片${index + 1}`}
              width={100}
              height={100}
              style={{
                objectFit: 'cover',
                borderRadius: '4px',
                border: '1px solid #d9d9d9'
              }}
            />
          ))}
        </div>
      </div>
    );
  };

  const renderTags = () => {
    const { tags } = parsedFields;
    if (!tags.length) return null;

    return (
      <div style={{ marginTop: '16px' }}>
        <Text strong>标签信息：</Text>
        <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
          {tags.map((tag: string, index: number) => (
            <Tag key={index}>{tag}</Tag>
          ))}
        </div>
      </div>
    );
  };

  return (
    <Card title={isReadonly ? "商品详情" : "编辑商品信息"} style={{ width: '100%' }}>
      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
        onFinish={handleFormSubmit}
        disabled={isReadonly}
      >
        <Collapse
          defaultActiveKey={isReadonly ? ['basic', 'pricing', 'inventory'] : ['basic', 'pricing', 'inventory']}
          ghost
          size="small"
        >
          {/* 基础信息 */}
          <Panel header="基础信息" key="basic">
            {isReadonly ? (
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  {renderReadonlyField("商品ID", <Text copyable>{product.id}</Text>)}
                  {renderReadonlyField("商品名称", product.name)}
                  {product.subtitle && renderReadonlyField("商品副标题", product.subtitle)}
                  {renderReadonlyField("商品价格",
                    <Text style={{ fontSize: '18px', color: '#1890ff', fontWeight: 'bold' }}>
                      {formattedPrices.price}
                    </Text>
                  )}
                  {formattedPrices.originalPrice && renderReadonlyField("原价",
                    <Text style={{ textDecoration: 'line-through', color: '#999' }}>
                      {formattedPrices.originalPrice}
                    </Text>
                  )}
                  {formattedPrices.depositPrice && renderReadonlyField("押金",
                    <Text style={{ color: '#fa8c16' }}>
                      {formattedPrices.depositPrice}
                    </Text>
                  )}
                  {renderReadonlyField("库存数量",
                    <Text style={{
                      color: stockStatus.stockColor,
                      fontWeight: stockStatus.stockWeight as any
                    }}>
                      {stockStatus.stockText}
                    </Text>
                  )}
                  {renderReadonlyField("商品状态",
                    <Text style={{ color: statusFormat.color, fontWeight: 'bold' }}>
                      {statusFormat.text}
                    </Text>
                  )}
                </Col>
                <Col span={12}>
                  {renderReadonlyField("规格", product.specification)}
                  {product.waterSource && renderReadonlyField("水源地", product.waterSource)}
                  {product.phValue !== undefined && renderReadonlyField("PH值", product.phValue)}
                  {product.mineralContent && renderReadonlyField("矿物质含量", product.mineralContent)}
                  {renderReadonlyField("销量", product.salesVolume)}
                  {renderReadonlyField("排序权重", product.sortOrder)}
                  {renderReadonlyField("创建时间", new Date(product.createdAt).toLocaleString('zh-CN'))}
                  {renderReadonlyField("更新时间", new Date(product.updatedAt).toLocaleString('zh-CN'))}
                </Col>
              </Row>
            ) : (
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
            )}

            {/* 封面图片 */}
            {product.coverImageUrl && (
              <div style={{ marginTop: '16px' }}>
                <Text strong>封面图片：</Text>
                <div style={{ marginTop: '8px' }}>
                  <Image
                    src={product.coverImageUrl}
                    alt="商品封面"
                    width={200}
                    height={200}
                    style={{
                      objectFit: 'cover',
                      borderRadius: '8px',
                      border: '1px solid #d9d9d9'
                    }}
                  />
                </div>
              </div>
            )}

            {renderImageGallery()}
            {renderCertificateImages()}
          </Panel>

          {/* 价格信息 */}
          <Panel header="价格信息" key="pricing">
            {isReadonly ? (
              <Row gutter={16}>
                <Col span={8}>
                  {renderReadonlyField("销售价格",
                    <Text style={{ fontSize: '18px', color: '#1890ff', fontWeight: 'bold' }}>
                      {formattedPrices.price}
                    </Text>
                  )}
                </Col>
                {formattedPrices.originalPrice && (
                  <Col span={8}>
                    {renderReadonlyField("原价", formattedPrices.originalPrice)}
                  </Col>
                )}
                {formattedPrices.depositPrice && (
                  <Col span={8}>
                    {renderReadonlyField("押金", formattedPrices.depositPrice)}
                  </Col>
                )}
              </Row>
            ) : (
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
            )}
          </Panel>

          {/* 库存与销售 */}
          <Panel header="库存与销售" key="inventory">
            {isReadonly ? (
              <Row gutter={16}>
                <Col span={8}>
                  {renderReadonlyField("库存数量",
                    <Text style={{
                      color: stockStatus.stockColor,
                      fontWeight: stockStatus.stockWeight as any
                    }}>
                      {stockStatus.stockText}
                    </Text>
                  )}
                </Col>
                <Col span={8}>
                  {renderReadonlyField("销量", product.salesVolume)}
                </Col>
                <Col span={8}>
                  {renderReadonlyField("排序权重", product.sortOrder)}
                </Col>
              </Row>
            ) : (
              <>
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
              </>
            )}
          </Panel>

          {/* 商品详情 */}
          <Panel header="商品详情" key="details" forceRender>
            {isReadonly ? (
              (product.description || product.detailContent) ? (
                <div>
                  {product.description && (
                    <div style={{ marginBottom: '16px' }}>
                      <Text strong>商品描述：</Text>
                      <div style={{ marginTop: '8px', whiteSpace: 'pre-wrap', lineHeight: '1.6' }}>
                        {product.description}
                      </div>
                    </div>
                  )}
                  {product.detailContent && (
                    <div>
                      <Text strong>商品详情：</Text>
                      <div style={{ marginTop: '8px' }} dangerouslySetInnerHTML={{ __html: product.detailContent }} />
                    </div>
                  )}
                </div>
              ) : (
                <Text type="secondary">暂无商品详情</Text>
              )
            ) : (
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
            )}
          </Panel>

          {/* 媒体资源 - 只在编辑模式下显示 */}
          {!isReadonly && (
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
          )}

          {/* 设置 - 只在编辑模式下显示 */}
          {!isReadonly && (
            <Panel header="设置" key="settings" forceRender>
              <Form.Item
                label="商品标签"
                name="tags"
                rules={[
                  { max: 1000, message: '标签JSON长度不能超过1000个字符' }
                ]}
              >
                <TagsEditor maxLength={1000} />
              </Form.Item>

              <Form.Item
                label="配送设置"
                name="deliverySettings"
                rules={[
                  { max: 2000, message: '配送设置JSON长度不能超过2000个字符' }
                ]}
              >
                <DeliverySettingsEditor maxLength={2000} />
              </Form.Item>
            </Panel>
          )}

          {/* 标签信息 - 只读模式下显示 */}
          {isReadonly && renderTags()}
        </Collapse>
      </Form>
    </Card>
  );
};