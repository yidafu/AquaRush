import React from 'react';
import { Form, Input, Select, Modal } from 'antd';
import type { AdminFormData, FormModalProps } from './types';

const AdminForm: React.FC<FormModalProps<AdminFormData>> = ({
  open,
  onClose,
  record,
  onSubmit,
  loading = false,
}) => {
  const [form] = Form.useForm();

  React.useEffect(() => {
    if (record) {
      form.setFieldsValue({
        username: record.username,
        realName: record.realName,
        phone: record.phone,
        role: record.role,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        role: 'NORMAL_ADMIN',
      });
    }
  }, [record, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      await onSubmit(values);
      form.resetFields();
    } catch (error) {
      console.error('Form validation failed:', error);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title={record ? '编辑管理员' : '新增管理员'}
      open={open}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          role: 'NORMAL_ADMIN',
        }}
      >
        <Form.Item
          label="用户名"
          name="username"
          rules={[
            { required: true, message: '请输入用户名' },
            { min: 3, max: 20, message: '用户名长度为3-20个字符' },
            { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线' },
          ]}
        >
          <Input placeholder="请输入用户名" disabled={!!record} />
        </Form.Item>

        <Form.Item
          label="真实姓名"
          name="realName"
          rules={[
            { max: 50, message: '真实姓名最多50个字符' },
            { pattern: /^[\u4e00-\u9fa5a-zA-Z\s]+$/, message: '真实姓名只能包含中文、英文字母和空格' },
          ]}
        >
          <Input placeholder="请输入真实姓名" />
        </Form.Item>

        <Form.Item
          label="手机号"
          name="phone"
          rules={[
            { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号' },
          ]}
        >
          <Input placeholder="请输入手机号" />
        </Form.Item>

        {!record && (
          <Form.Item
            label="密码"
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, max: 20, message: '密码长度为6-20个字符' },
            ]}
          >
            <Input.Password placeholder="请输入密码" />
          </Form.Item>
        )}

        <Form.Item
          label="角色"
          name="role"
          rules={[{ required: true, message: '请选择角色' }]}
        >
          <Select placeholder="请选择角色">
            <Select.Option value="NORMAL_ADMIN">普通管理员</Select.Option>
            <Select.Option value="ADMIN">管理员</Select.Option>
            <Select.Option value="SUPER_ADMIN">超级管理员</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AdminForm;