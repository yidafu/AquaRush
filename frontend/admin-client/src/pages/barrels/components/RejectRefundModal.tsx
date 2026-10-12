import React, { useEffect } from 'react';
import { Modal, Form, Input } from '@/ui';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import type { DepositRefundApplication } from '../../../types/barrel';

// 局部色值 #666 为原有辅助文字灰,共享 token 无对应值,保持原样(报告 (a))
const styles = stylex.create({
  summary: {
    marginBottom: '12px',
    color: '#666',
  },
});

export interface RejectRefundFormValues {
  reason: string;
}

export interface RejectRefundModalProps {
  /** 驳回目标;null = 关闭 */
  target: DepositRefundApplication | null;
  confirmLoading?: boolean;
  onCancel: () => void;
  /** 表单提交(页面负责调用 rejectDepositRefund) */
  onSubmit: (values: RejectRefundFormValues) => void;
}

/** 驳回退押申请弹窗(必填驳回原因,barrels/index.tsx 拆分) */
const RejectRefundModal: React.FC<RejectRefundModalProps> = ({
  target,
  confirmLoading = false,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<RejectRefundFormValues>();

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
      title="驳回退押申请"
      open={!!target}
      onCancel={handleCancel}
      onOk={handleOk}
      okText="确认驳回"
      okButtonProps={{ danger: true }}
      confirmLoading={confirmLoading}
    >
      {target && (
        <div {...stylex.props(styles.summary)}>
          {target.specification} × {target.bucketCount} 桶 · 退押金额{' '}
          {displayCents(Number(target.depositCents))}
          <br />
          驳回后申请终止,押金保留在桶账中。
        </div>
      )}
      <Form form={form}>
        <Form.Item
          name="reason"
          label="驳回原因"
          rules={[
            { required: true, message: '请输入驳回原因' },
            { max: 500, message: '原因不能超过500个字符' },
          ]}
        >
          <Input.TextArea rows={3} maxLength={500} placeholder="请输入驳回原因(如:空桶破损/数量不符)" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default RejectRefundModal;
