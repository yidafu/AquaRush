import React from 'react';
import {
  Form,
  Input,
  InputNumber,
  Select,
  Divider,
  Collapse,
  Row,
  Col,
  Space,
} from '@/ui';
import { SettingOutlined } from '@/ui/icons';
import { SingleImageUpload, MultiImageUpload } from '../../../components/ImageUpload';
import { SimpleRichTextEditor } from '../../../components/RichTextEditor';
import { TagsEditor } from '../../../components/JsonEditor';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  manageLink: {
    fontSize: '12px',
  },
  fullWidth: {
    width: '100%',
  },
  jsonArea: {
    fontFamily: 'monospace',
  },
});

const { TextArea } = Input;
const { Panel } = Collapse;

// 下拉选项过滤(忽略大小写)
const filterOption = (inputValue: string, option?: { value: string }) =>
  (option?.value ?? '').toUpperCase().includes(inputValue.toUpperCase());

export interface ProductFormFieldsProps {
  /** 规格选项(后台配置字典,F-PRO-009) */
  specificationOptions?: string[];
  /** 水源地选项(后台配置字典,F-PRO-009) */
  waterSourceOptions?: string[];
  /** 跳转「规格/水源地配置」页(管理下拉候选) */
  onManageOptions?: () => void;
}

/** 表单标签上的「管理」链接(点击跳转配置页) */
const manageLabel = (text: string, onManageOptions?: () => void): React.ReactNode => (
  <Space size={4}>
    {text}
    <a
      {...stylex.props(styles.manageLink)}
      onClick={(e) => {
        e.preventDefault();
        onManageOptions?.();
      }}
    >
      <SettingOutlined /> 管理
    </a>
  </Space>
);

/**
 * 商品表单字段(新增/编辑页共用):
 * 基础信息、价格信息、库存与销售、商品详情、媒体资源、设置
 * 由 ProductFormPage 包在 <Form> 中渲染;校验规则与后端 GraphQL 约束保持一致。
 * 规格/水源地为后台可配置字典下拉(F-PRO-009),不再使用前端硬编码候选。
 */
export const ProductFormFields: React.FC<ProductFormFieldsProps> = ({
  specificationOptions = [],
  waterSourceOptions = [],
  onManageOptions,
}) => (
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
            label={manageLabel('规格', onManageOptions)}
            name="specification"
            rules={[
              { required: true, message: '请选择商品规格' },
              { min: 1, max: 100, message: '规格长度应在1-100个字符之间' }
            ]}
          >
            <Select
              showSearch
              placeholder="请选择商品规格"
              options={specificationOptions.map((value) => ({ value, label: value }))}
              filterOption={filterOption}
              notFoundContent="暂无配置,请点击「管理」添加规格选项"
            />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item
            label={manageLabel('水源地', onManageOptions)}
            name="waterSource"
            rules={[
              { max: 200, message: '水源地长度不能超过200个字符' }
            ]}
          >
            <Select
              showSearch
              allowClear
              placeholder="请选择水源地"
              options={waterSourceOptions.map((value) => ({ value, label: value }))}
              filterOption={filterOption}
              notFoundContent="暂无配置,请点击「管理」添加水源地选项"
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
              {...stylex.props(styles.fullWidth)}
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
              {...stylex.props(styles.fullWidth)}
              addonAfter="元"
            />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item
            label="排序权重"
            name="sortOrder"
          >
            <InputNumber
              min={0}
              max={9999}
              precision={0}
              placeholder="0"
              {...stylex.props(styles.fullWidth)}
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
              {...stylex.props(styles.fullWidth)}
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
              disabled
              {...stylex.props(styles.fullWidth)}
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
              {...stylex.props(styles.fullWidth)}
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
            rules={[
              { max: 2000, message: '图片URL长度不能超过2000个字符' }
            ]}
          >
            <MultiImageUpload maxCount={10} />
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
            <MultiImageUpload maxCount={5} />
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
              // warningOnly:URL 不合法时仅提示、不阻断提交(门面原生支持该键)
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
              {...stylex.props(styles.jsonArea)}
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
              {...stylex.props(styles.jsonArea)}
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
      >
        <TagsEditor />
      </Form.Item>
    </Panel>
  </Collapse>
);
