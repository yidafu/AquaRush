import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber } from 'antd';
import { centsToYuan } from '@aquarush/common';
import type { BarrelFee } from '../../../types/barrel';

export interface BarrelFeeFormValues {
  specification: string;
  price: number;
  sortOrder?: number;
}

export interface BarrelFeeFormModalProps {
  open: boolean;
  /** 编辑对象;null = 新增 */
  editing: BarrelFee | null;
  onCancel: () => void;
  /** 表单校验通过后提交(页面负责构造 payload 并调用 mutation) */
  onSubmit: (values: BarrelFeeFormValues) => void;
}

/** 价目新增/编辑弹窗(G06,barrels/index.tsx 拆分) */
const BarrelFeeFormModal: React.FC<BarrelFeeFormModalProps> = ({
  open,
  editing,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<BarrelFeeFormValues>();

  // 打开时回填(编辑)或清空(新增)
  useEffect(() => {
    if (!open) return;
    if (editing) {
      form.setFieldsValue({
        specification: editing.specification,
        price: centsToYuan(Number(editing.priceCents)),
        sortOrder: editing.sortOrder,
      });
    } else {
      form.resetFields();
    }
  }, [open, editing, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit(values);
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  return (
    <Modal
      title={editing ? '编辑水桶费价目' : '新增水桶费价目'}
      open={open}
      onCancel={handleCancel}
      onOk={handleOk}
      okText={editing ? '保存' : '创建'}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="specification"
          label="规格"
          rules={[{ required: true, message: '请输入规格(如 19L/桶)' }, { max: 100, message: '规格不能超过100个字符' }]}
        >
          <Input placeholder="如 19L/桶" />
        </Form.Item>
        <Form.Item
          name="price"
          label="价格(元)"
          rules={[{ required: true, message: '请输入价格' }]}
        >
          <InputNumber min={0} max={99999} precision={2} style={{ width: '100%' }} placeholder="一次性塑料水桶费用" />
        </Form.Item>
        <Form.Item name="sortOrder" label="排序权重(升序)">
          <InputNumber min={0} max={9999} style={{ width: '100%' }} placeholder="默认 999" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default BarrelFeeFormModal;
