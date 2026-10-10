import React, { useEffect } from 'react';
import { Form, Input, InputNumber, Select } from 'antd';
import { centsToYuan } from '@aquarush/common';
import type { DepositPrice } from '../../../types/barrel';
import FormModal from '@/components/FormModal';
import { DEPOSIT_PRICE_STATUS_MAP } from './constants';

export interface DepositPriceFormValues {
  specification: string;
  deposit: number;
  status: 'ENABLED' | 'DISABLED';
  sortOrder?: number;
}

export interface DepositPriceFormModalProps {
  open: boolean;
  /** 编辑对象;null = 新增 */
  editing: DepositPrice | null;
  /** 提交中(confirmLoading) */
  submitting?: boolean;
  onCancel: () => void;
  /** 表单校验通过后提交(页面负责构造 payload 并调用 upsertDepositPrice) */
  onSubmit: (values: DepositPriceFormValues) => void;
}

/** 押金价目新增/编辑弹窗(G06;金额输入元,页面提交时经 yuanToCents 转分) */
const DepositPriceFormModal: React.FC<DepositPriceFormModalProps> = ({
  open,
  editing,
  submitting = false,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<DepositPriceFormValues>();

  useEffect(() => {
    if (!open) return;
    if (editing) {
      form.setFieldsValue({
        specification: editing.specification,
        deposit: centsToYuan(Number(editing.depositCents)),
        status: editing.status,
        sortOrder: editing.sortOrder,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({ status: 'ENABLED' });
    }
  }, [open, editing, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit(values);
  };

  return (
    <FormModal
      open={open}
      record={editing}
      submitting={submitting}
      onCancel={onCancel}
      onOk={handleOk}
      noun="押金价目"
      okText={editing ? '保存' : '创建'}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="specification"
          label="规格"
          rules={[
            { required: true, message: '请输入规格(如 19L/桶)' },
            { max: 100, message: '规格不能超过100个字符' },
          ]}
        >
          <Input placeholder="如 19L/桶" />
        </Form.Item>
        <Form.Item
          name="deposit"
          label="押金(元)"
          rules={[{ required: true, message: '请输入押金金额' }]}
        >
          <InputNumber min={0} max={99999} precision={2} style={{ width: '100%' }} placeholder="循环水桶押金,如 40" />
        </Form.Item>
        <Form.Item name="status" label="状态">
          <Select
            options={Object.entries(DEPOSIT_PRICE_STATUS_MAP).map(([value, info]) => ({
              value,
              label: info.text,
            }))}
          />
        </Form.Item>
        <Form.Item name="sortOrder" label="排序权重(升序)">
          <InputNumber min={0} max={9999} style={{ width: '100%' }} placeholder="默认 999" />
        </Form.Item>
      </Form>
    </FormModal>
  );
};

export default DepositPriceFormModal;
