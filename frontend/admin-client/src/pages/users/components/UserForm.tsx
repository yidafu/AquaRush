import React from 'react';
import { Form, Input, Select, Modal } from '@aquarush/admin-ui';
import { AvatarUpload } from '../../../components/AvatarUpload';
import type { UserFormData, FormModalProps } from './types';

const UserForm: React.FC<FormModalProps<UserFormData>> = ({
  open,
  onClose,
  record,
  onSubmit,
  loading = false,
}) => {
  const [form] = Form.useForm<UserFormData>();
  // 头像/昵称联动:未上传头像时预览根据昵称自动生成的默认头像
  const nickname = Form.useWatch('nickname', form);

  React.useEffect(() => {
    if (record) {
      form.setFieldsValue({
        nickname: record.nickname,
        phone: record.phone,
        wechatOpenId: record.wechatOpenId,
        avatarUrl: record.avatarUrl,
        status: record.status,
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        status: 'ACTIVE',
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
      title={record ? '编辑用户' : '新增用户'}
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
          status: 'ACTIVE',
        }}
      >
        <Form.Item
          label="头像"
          name="avatarUrl"
        >
          <AvatarUpload name={nickname} fallbackName="用户" />
        </Form.Item>

        <Form.Item
          label="昵称"
          name="nickname"
          rules={[
            { max: 50, message: '昵称最多50个字符' },
          ]}
        >
          <Input placeholder="请输入昵称" />
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

        <Form.Item
          label="微信OpenID"
          name="wechatOpenId"
          extra={record ? undefined : '后台创建的用户由系统自动生成占位 OpenID（微信登录用户无需填写）'}
        >
          <Input placeholder={record ? undefined : '系统自动生成'} disabled={!record} />
        </Form.Item>

        <Form.Item
          label="状态"
          name="status"
          rules={[{ required: true, message: '请选择状态' }]}
        >
          <Select placeholder="请选择状态">
            <Select.Option value="ACTIVE">正常</Select.Option>
            <Select.Option value="INACTIVE">未激活</Select.Option>
            <Select.Option value="SUSPENDED">已暂停</Select.Option>
            <Select.Option value="DELETED">已删除</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default UserForm;