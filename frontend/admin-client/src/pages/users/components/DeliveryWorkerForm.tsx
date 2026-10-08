import React from 'react';
import { Form, Input, Select, Switch, Modal } from 'antd';
import { AvatarUpload } from '../../../components/AvatarUpload';
import type { DeliveryWorkerFormData, FormModalProps } from './types';

const DeliveryWorkerForm: React.FC<FormModalProps<DeliveryWorkerFormData>> = ({
  open,
  onClose,
  record,
  onSubmit,
  loading = false,
}) => {
  const [form] = Form.useForm();
  // 头像/姓名联动:未上传头像时预览根据姓名自动生成的默认头像
  const name = Form.useWatch('name', form);

  React.useEffect(() => {
    if (record) {
      form.setFieldsValue({
        name: record.name,
        phone: record.phone,
        wechatOpenId: record.wechatOpenId,
        avatarUrl: record.avatarUrl,
        status: record.status,
        isAvailable: record.isAvailable,
        accountStatus: record.accountStatus ?? 'ACTIVE',
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        status: 'OFFLINE',
        isAvailable: true,
        accountStatus: 'ACTIVE',
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
      title={record ? '编辑送水员' : '新增送水员'}
      open={open}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={700}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          status: 'OFFLINE',
          isAvailable: true,
          accountStatus: 'ACTIVE',
        }}
      >
        <Form.Item
          label="头像"
          name="avatarUrl"
        >
          <AvatarUpload name={name} fallbackName="送水员" />
        </Form.Item>

        <Form.Item
          label="姓名"
          name="name"
          rules={[
            { required: true, message: '请输入姓名' },
            { min: 2, max: 20, message: '姓名长度为2-20个字符' },
            {
            pattern: /^[\u4e00-\u9fa5a-zA-Z\u00b7]+(?:[ \u3000][\u4e00-\u9fa5a-zA-Z\u00b7]+)*$/,
            message: '姓名只能包含中文、英文字母、空格和·',
          },
          ]}
        >
          <Input placeholder="请输入姓名" />
        </Form.Item>

        <Form.Item
          label="手机号"
          name="phone"
          rules={[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号' },
          ]}
        >
          <Input placeholder="请输入手机号" />
        </Form.Item>

        <Form.Item
          label="微信OpenID"
          name="wechatOpenId"
        >
          <Input placeholder="请输入微信OpenID" disabled />
        </Form.Item>

        {!record && (
          <Form.Item
            label="登录密码"
            name="password"
            rules={[
              { required: true, message: '请输入登录密码' },
              { min: 6, max: 20, message: '密码长度为6-20个字符' },
            ]}
          >
            <Input.Password placeholder="请输入登录密码" />
          </Form.Item>
        )}

        <Form.Item
          label="状态"
          name="status"
          rules={[{ required: true, message: '请选择状态' }]}
        >
          <Select placeholder="请选择状态">
            <Select.Option value="ONLINE">在线</Select.Option>
            <Select.Option value="OFFLINE">离线</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="账号状态"
          name="accountStatus"
          rules={[{ required: true, message: '请选择账号状态' }]}
        >
          <Select placeholder="请选择账号状态">
            <Select.Option value="ACTIVE">正常</Select.Option>
            <Select.Option value="DISABLED">停用</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="是否可接单"
          name="isAvailable"
          valuePropName="checked"
        >
          <Switch checkedChildren="可接单" unCheckedChildren="不可接单" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DeliveryWorkerForm;
