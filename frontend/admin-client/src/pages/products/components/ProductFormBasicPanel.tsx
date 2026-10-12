import React from 'react';
import { Form, Input, Select, Row, Col, Space } from '@aquarush/admin-ui';
import { SettingOutlined } from '@aquarush/admin-ui/icons';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  manageLink: {
    fontSize: '12px',
  },
});

const { TextArea } = Input;

// 下拉选项过滤(忽略大小写)
const filterOption = (inputValue: string, option?: { value: string }) =>
  (option?.value ?? '').toUpperCase().includes(inputValue.toUpperCase());

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

export interface ProductBasicPanelProps {
  /** 规格选项(后台配置字典,F-PRO-009) */
  specificationOptions?: string[];
  /** 水源地选项(后台配置字典,F-PRO-009) */
  waterSourceOptions?: string[];
  /** 跳转「规格/水源地配置」页(管理下拉候选) */
  onManageOptions?: () => void;
}

/** 「基础信息」面板内容(由 ProductFormFields 包在 Collapse.Panel 中渲染) */
export const ProductBasicPanel: React.FC<ProductBasicPanelProps> = ({
  specificationOptions = [],
  waterSourceOptions = [],
  onManageOptions,
}) => (
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
);
