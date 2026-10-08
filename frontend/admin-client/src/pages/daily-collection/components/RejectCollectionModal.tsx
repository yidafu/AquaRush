import React, { useEffect } from 'react';
import { Modal, Form, Input } from 'antd';
import type { DailyCollectionRecord } from '../../../types/daily-collection';

export interface RejectCollectionModalProps {
  /** 驳回目标;null = 关闭 */
  target: DailyCollectionRecord | null;
  onCancel: () => void;
  /** 提交驳回(原因必填,G28) */
  onSubmit: (reason: string) => void;
}

/** 驳回收款记录弹窗(G28:争议日报驳回,必填原因,daily-collection/index.tsx 拆分) */
const RejectCollectionModal: React.FC<RejectCollectionModalProps> = ({
  target,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm<{ rejectReason: string }>();

  useEffect(() => {
    if (target) {
      form.resetFields();
    }
  }, [target, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit(values.rejectReason);
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  return (
    <Modal
      title="驳回收款记录"
      open={!!target}
      onCancel={handleCancel}
      onOk={handleOk}
      okText="确认驳回"
      okButtonProps={{ danger: true }}
    >
      {target && (
        <div style={{ marginBottom: 12, color: '#666' }}>
          配送员 {target.deliveryWorkerId} · {target.collectionDate}
          {target.notes ? ` · 差异说明: ${target.notes}` : ''}
        </div>
      )}
      <Form form={form}>
        <Form.Item
          name="rejectReason"
          rules={[{ required: true, message: '请填写驳回原因' }]}
        >
          <Input.TextArea rows={3} maxLength={500} placeholder="请输入驳回原因(必填)" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default RejectCollectionModal;
