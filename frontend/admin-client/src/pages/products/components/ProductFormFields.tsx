import React from 'react';
import {
  Form,
  InputNumber,
  Select,
  Collapse,
  Row,
  Col,
} from '@/ui';
import { ProductBasicPanel } from './ProductFormBasicPanel';
import {
  ProductDetailPanel,
  ProductMediaPanel,
  ProductSettingsPanel,
} from './ProductFormDetailPanels';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  fullWidth: {
    width: '100%',
  },
});

const { Panel } = Collapse;

export interface ProductFormFieldsProps {
  /** 规格选项(后台配置字典,F-PRO-009) */
  specificationOptions?: string[];
  /** 水源地选项(后台配置字典,F-PRO-009) */
  waterSourceOptions?: string[];
  /** 跳转「规格/水源地配置」页(管理下拉候选) */
  onManageOptions?: () => void;
}

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
      <ProductBasicPanel
        specificationOptions={specificationOptions}
        waterSourceOptions={waterSourceOptions}
        onManageOptions={onManageOptions}
      />
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
      <ProductDetailPanel />
    </Panel>

    {/* Media */}
    <Panel header="媒体资源" key="media" forceRender>
      <ProductMediaPanel />
    </Panel>

    {/* Settings */}
    <Panel header="设置" key="settings" forceRender>
      <ProductSettingsPanel />
    </Panel>
  </Collapse>
);
