import React, { useEffect } from 'react';
import { Modal, Form, Input } from 'antd';
import { displayCents } from '@aquarush/common';
import type { BarrelPurchase } from '../../../types/barrel';

export interface RefundBarrelFormValues {
  refundRemark?: string;
}

export interface RefundBarrelModalProps {
  /** 退还目标;null = 关闭 */
  target: BarrelPurchase | null;
  confirmLoading?: boolean;
  onCancel: () => void;
  /** 表单提交(页面负责调用 mutation) */
  onSubmit: (values: RefundBarrelFormValues) => void;
}

/** 退还水桶费弹窗(G06:管理员手动退还,barrels/index.tsx 拆分) */
const RefundBarrelModal: React.FC<RefundBarrelModalProps> = ({
  target,
  confirmLoading = false,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<RefundBarrelFormValues>();

  useEffect(() => {
    if (target) {
      form.resetFields();
    }
  }, [target, form]);

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
      title="退还水桶费"
      open={!!target}
      onCancel={handleCancel}
      onOk={handleOk}
      okText="确认退还"
      confirmLoading={confirmLoading}
    >
      {target && (
        <div style={{ marginBottom: 12, color: '#666' }}>
          购桶单 {target.orderNo} · 金额 {displayCents(Number(target.totalAmountCents))}
          <br />
          备注:水桶费为一次性费用,退还仅适用于退桶/停止服务等特殊情况。
        </div>
      )}
      <Form form={form}>
        <Form.Item name="refundRemark" label="退还备注">
          <Input.TextArea rows={3} maxLength={500} placeholder="请输入退还备注(如:空桶已退回)" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default RefundBarrelModal;
