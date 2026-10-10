import React from 'react';
import { Form, Input, Select, Switch, Modal } from 'antd';
import { AvatarUpload } from '../../../components/AvatarUpload';
import type { AdminFormData, FormModalProps } from './types';

/**
 * 后台账号表单(送水员档案并入 admin 账号表单表,spec/prd/07-user.md §F-USR-021 目标架构):
 * role=DELIVERY_WORKER 时展示配送字段(头像/在线状态/接单权限),用户名可不填(后端自动生成 delivery_{账号ID});
 * 纯管理账号(SUPER_ADMIN/ADMIN)相关配送字段留空。
 */
const AdminForm: React.FC<FormModalProps<AdminFormData>> = ({
  open,
  onClose,
  record,
  onSubmit,
  loading = false,
  isSuperAdmin = true,
}) => {
  const [form] = Form.useForm();
  const selectedRole = Form.useWatch('role', form);
  // 头像/用户名联动:未上传头像时预览根据用户名自动生成的默认头像
  const username = Form.useWatch('username', form);
  const realName = Form.useWatch('realName', form);

  const isDeliveryWorker = selectedRole === 'DELIVERY_WORKER';

  React.useEffect(() => {
    if (record) {
      form.setFieldsValue({
        username: record.username,
        realName: record.realName,
        phone: record.phone,
        role: record.role,
        avatarUrl: record.avatarUrl,
        status: record.status ?? 'OFFLINE',
        isAvailable: record.isAvailable ?? true,
        accountStatus: record.accountStatus ?? 'ACTIVE',
      });
    } else {
      form.resetFields();
      form.setFieldsValue({
        role: 'ADMIN',
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
      title={record ? '编辑账号' : '新增账号'}
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
          role: 'ADMIN',
          status: 'OFFLINE',
          isAvailable: true,
          accountStatus: 'ACTIVE',
        }}
      >
        <Form.Item
          label="用户名"
          name="username"
          extra={isDeliveryWorker ? '送水员用户名由系统自动生成（delivery_{账号ID}）' : undefined}
          rules={[
            {
              validator: (_, value) => {
                if (isDeliveryWorker && !value) {
                  return Promise.resolve();
                }
                if (!value) {
                  return Promise.reject(new Error('请输入用户名'));
                }
                if (value.length < 3 || value.length > 20) {
                  return Promise.reject(new Error('用户名长度为3-20个字符'));
                }
                if (!/^[a-zA-Z0-9_]+$/.test(value)) {
                  return Promise.reject(new Error('用户名只能包含字母、数字和下划线'));
                }
                return Promise.resolve();
              },
            },
          ]}
        >
          <Input placeholder={isDeliveryWorker ? '留空自动生成' : '请输入用户名'} disabled={!!record} />
        </Form.Item>

        <Form.Item
          label="真实姓名"
          name="realName"
          rules={[
            { max: 50, message: '真实姓名最多50个字符' },
            {
              pattern: /^[\u4e00-\u9fa5a-zA-Z\u00b7]+(?:[ \u3000][\u4e00-\u9fa5a-zA-Z\u00b7]+)*$/,
              message: '真实姓名只能包含中文、英文字母、空格和·',
            },
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
          <Select
            placeholder="请选择角色"
            disabled={!isSuperAdmin}
          >
            <Select.Option value="ADMIN">管理员</Select.Option>
            <Select.Option value="SUPER_ADMIN">超级管理员</Select.Option>
            <Select.Option value="DELIVERY_WORKER">送水员</Select.Option>
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

        {/* 头像所有角色可用;在线状态/接单权限仅送水员(spec/prd/07-user.md §F-USR-020/021) */}
        <Form.Item label="头像" name="avatarUrl">
          <AvatarUpload
            name={username || realName}
            fallbackName={isDeliveryWorker ? '送水员' : '管理员'}
          />
        </Form.Item>

        {isDeliveryWorker && (
          <>
            <Form.Item
              label="在线状态"
              name="status"
              rules={[{ required: true, message: '请选择在线状态' }]}
            >
              <Select placeholder="请选择在线状态">
                <Select.Option value="ONLINE">在线</Select.Option>
                <Select.Option value="OFFLINE">离线</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item label="接单权限" name="isAvailable" valuePropName="checked">
              <Switch checkedChildren="可接单" unCheckedChildren="不可接单" />
            </Form.Item>
          </>
        )}
      </Form>
    </Modal>
  );
};

export default AdminForm;
