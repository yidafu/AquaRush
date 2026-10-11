import React, { useEffect } from 'react';
import { Modal, Form, Input, Switch, Typography } from '@/ui';
import type { TemplateRow } from './constants';
import { TEMPLATE_ID_PATTERN } from './constants';

export interface TemplateEditValues {
  templateId: string;
  enabled: boolean;
}

export interface TemplateEditModalProps {
  /** 编辑目标;null = 关闭 */
  editing: TemplateRow | null;
  saving: boolean;
  canWrite: boolean;
  onClose: () => void;
  /** 表单校验通过后提交(页面负责调用 mutation) */
  onSubmit: (values: TemplateEditValues) => void;
}

/** 微信模板 ID 编辑弹窗(system/notifications/index.tsx 拆分) */
const TemplateEditModal: React.FC<TemplateEditModalProps> = ({
  editing,
  saving,
  canWrite,
  onClose,
  onSubmit,
}) => {
  const [form] = Form.useForm<TemplateEditValues>();

  // 打开时回填当前值
  useEffect(() => {
    if (!editing) return;
    form.setFieldsValue({
      templateId: editing.templateId ?? '',
      enabled: editing.enabled,
    });
  }, [editing, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit(values);
  };

  return (
    <Modal
      title={`编辑模板${editing ? `:${editing.scenario.label}` : ''}`}
      open={!!editing}
      onOk={handleOk}
      onCancel={onClose}
      okText="保存"
      cancelText="取消"
      confirmLoading={saving}
      okButtonProps={{ disabled: !canWrite }}
    >
      {editing && (
        <Form form={form} layout="vertical">
          <Form.Item
            name="templateId"
            label="模板 ID"
            extra="粘贴自微信公众平台订阅消息模板 ID"
            rules={[
              { required: true, message: '模板 ID 不能为空' },
              { pattern: TEMPLATE_ID_PATTERN, message: '模板 ID 格式不正确' },
            ]}
          >
            <Input maxLength={64} placeholder="请输入微信订阅消息模板 ID" disabled={!canWrite} />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch disabled={!canWrite} checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
          <Form.Item label="业务场景说明">
            <Typography.Text type="secondary">{editing.scenario.description}</Typography.Text>
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
};

export default TemplateEditModal;
