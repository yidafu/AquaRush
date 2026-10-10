import React, { useEffect } from 'react';
import { Form, Input, Modal } from 'antd';

interface ResetPasswordModalProps {
  /** 目标账号展示名(标题文案用) */
  targetName?: string;
  open: boolean;
  confirmLoading?: boolean;
  onOk: (newPassword?: string) => void;
  onCancel: () => void;
}

/**
 * 重置密码弹窗(F-USR-020/021,G11 落地)
 * 新密码可留空:后端重置为默认密码 123456(与 spec/prd/07-user.md §F-USR-010/021 一致)
 */
const ResetPasswordModal: React.FC<ResetPasswordModalProps> = ({
  targetName,
  open,
  confirmLoading = false,
  onOk,
  onCancel,
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      form.resetFields();
    }
  }, [open, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      onOk(values.newPassword || undefined);
    } catch {
      // 校验失败不提交
    }
  };

  return (
    <Modal
      title={`重置密码 - ${targetName || ''}`}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={confirmLoading}
      okText="重置"
      cancelText="取消"
      destroyOnClose
    >
      <div style={{ marginBottom: 12, color: '#888' }}>
        新密码留空则重置为默认密码 <b>123456</b>（初始密码，建议登录后修改）。
      </div>
      <Form form={form} layout="vertical">
        <Form.Item
          label="新密码（可选）"
          name="newPassword"
          rules={[
            { min: 6, max: 20, message: '密码长度为6-20个字符' },
          ]}
        >
          <Input.Password placeholder="留空则重置为默认密码 123456" autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ResetPasswordModal;
