import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Card,
  Table,
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Switch,
  Space,
  Tag,
  Popconfirm,
  Tabs,
  Alert,
  message,
} from '@/ui';
import { PlusOutlined } from '@/ui/icons';
import { formatDateTime } from '@aquarush/common';
import {
  useProductOptionsConfig,
  useCreateProductOption,
  useUpdateProductOption,
  useDeleteProductOption,
} from '../../../services/product-graphql';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  infoAlert: {
    marginBottom: '16px',
  },
  fullWidth: {
    width: '100%',
  },
});

type OptionType = 'SPECIFICATION' | 'WATER_SOURCE';

interface ProductOptionRow {
  id: string;
  optionType: OptionType;
  value: string;
  sortOrder: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

interface OptionFormValues {
  value: string;
  sortOrder?: number;
  enabled: boolean;
}

const TYPE_LABEL: Record<OptionType, string> = {
  SPECIFICATION: '规格',
  WATER_SOURCE: '水源地',
};

/**
 * 规格/水源地选项配置页(F-PRO-009):
 * 商品表单「规格 / 水源地」下拉选项的后台配置字典,替换原前端硬编码候选;
 * 禁用项不再出现在商品表单下拉中(存量商品值仍回显),删除仅移除候选,不影响已引用该值的商品。
 */
const ProductOptionsPage: React.FC = () => {
  const [loadOptions, { data, loading }] = useProductOptionsConfig();
  const [createOption] = useCreateProductOption();
  const [updateOption] = useUpdateProductOption();
  const [deleteOption] = useDeleteProductOption();

  const [activeType, setActiveType] = useState<OptionType>('SPECIFICATION');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ProductOptionRow | null>(null);
  const [form] = Form.useForm<OptionFormValues>();

  useEffect(() => {
    loadOptions();
  }, [loadOptions]);

  const rows = useMemo<ProductOptionRow[]>(() => {
    const source = activeType === 'SPECIFICATION' ? data?.specificationOptions : data?.waterSourceOptions;
    return (source ?? []) as ProductOptionRow[];
  }, [activeType, data]);

  const openCreate = useCallback(() => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ enabled: true });
    setModalOpen(true);
  }, [form]);

  const openEdit = useCallback(
    (row: ProductOptionRow) => {
      setEditing(row);
      form.setFieldsValue({ value: row.value, sortOrder: row.sortOrder, enabled: row.enabled });
      setModalOpen(true);
    },
    [form]
  );

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editing) {
      await updateOption({
        variables: {
          id: editing.id,
          input: {
            value: values.value.trim(),
            sortOrder: values.sortOrder,
            enabled: values.enabled,
          },
        },
      });
    } else {
      await createOption({
        variables: {
          input: {
            optionType: activeType,
            value: values.value.trim(),
            sortOrder: values.sortOrder,
            enabled: values.enabled,
          },
        },
      });
    }
    setModalOpen(false);
  };

  const handleDelete = async (row: ProductOptionRow) => {
    try {
      await deleteOption({ variables: { id: row.id } });
      message.success('选项删除成功');
    } catch {
      // 错误提示已由 mutation hook 的 onError 统一处理
    }
  };

  const columns = [
    {
      title: '选项值',
      dataIndex: 'value',
      key: 'value',
      render: (value: string) => <Tag>{value}</Tag>,
    },
    {
      title: '排序号',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 100,
      render: (enabled: boolean) => (enabled ? <Tag color="green">启用</Tag> : <Tag color="default">禁用</Tag>),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_: unknown, row: ProductOptionRow) => (
        <Space>
          <Button type="link" size="small" onClick={() => openEdit(row)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除该选项吗?"
            description="删除后商品表单下拉不再展示;已引用该值的商品不受影响"
            okText="删除"
            cancelText="取消"
            onConfirm={() => handleDelete(row)}
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card title="规格/水源地配置" loading={loading}>
      <Alert
        type="info"
        showIcon
        {...stylex.props(styles.infoAlert)}
        message="商品新增/编辑表单的「规格」「水源地」下拉选项来源于本页配置(替换原前端硬编码候选)。禁用项不在商品表单下拉中展示,存量商品值仍正常回显;删除仅移除候选,不影响已引用该值的商品。"
      />
      <Tabs
        activeKey={activeType}
        onChange={(key) => setActiveType(key as OptionType)}
        items={[
          { key: 'SPECIFICATION', label: `规格 (${(data?.specificationOptions ?? []).length})` },
          { key: 'WATER_SOURCE', label: `水源地 (${(data?.waterSourceOptions ?? []).length})` },
        ]}
        tabBarExtraContent={
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增{TYPE_LABEL[activeType]}
          </Button>
        }
      />
      <Table<ProductOptionRow>
        rowKey="id"
        columns={columns}
        dataSource={rows}
        pagination={false}
        locale={{ emptyText: '暂无配置选项,点击右上角「新增」添加' }}
      />

      <Modal
        title={editing ? `编辑${TYPE_LABEL[editing.optionType]}` : `新增${TYPE_LABEL[activeType]}`}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="value"
            label="选项值"
            rules={[
              { required: true, message: '请输入选项值' },
              { min: 1, max: 100, message: '选项值长度应在1-100个字符之间' },
            ]}
          >
            <Input placeholder={activeType === 'SPECIFICATION' ? '如：19L' : '如：长白山'} maxLength={100} />
          </Form.Item>
          <Form.Item
            name="sortOrder"
            label="排序号"
            extra="同类型内按排序号升序展示,越小越靠前"
          >
            <InputNumber min={0} precision={0} {...stylex.props(styles.fullWidth)} placeholder="缺省排到末尾" />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default ProductOptionsPage;
