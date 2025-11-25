import React, { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined, EditOutlined, LockOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

interface User {
  id: string;
  username: string;
  phone: string;
  nickname?: string;
  role: 'ADMIN' | 'CUSTOMER' | 'DELIVERY';
  status: 'ACTIVE' | 'INACTIVE' | 'BANNED';
  createdAt: string;
}

const Users: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();

  const columns: ColumnsType<User> = [
    { 
      title: '用户名', 
      dataIndex: 'username', 
      key: 'username',
      width: 120,
    },
    { 
      title: '昵称', 
      dataIndex: 'nickname', 
      key: 'nickname',
      width: 120,
    },
    { 
      title: '手机号', 
      dataIndex: 'phone', 
      key: 'phone',
      width: 130,
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 100,
      render: (role: string) => {
        const roleMap = {
          ADMIN: { color: 'red', text: '管理员' },
          CUSTOMER: { color: 'blue', text: '客户' },
          DELIVERY: { color: 'green', text: '配送员' },
        };
        const config = roleMap[role as keyof typeof roleMap];
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const statusMap = {
          ACTIVE: { color: 'success', text: '正常' },
          INACTIVE: { color: 'default', text: '未激活' },
          BANNED: { color: 'error', text: '已禁用' },
        };
        const config = statusMap[status as keyof typeof statusMap];
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    { 
      title: '注册时间', 
      dataIndex: 'createdAt', 
      key: 'createdAt',
      width: 160,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button 
            type="link" 
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button 
            type="link" 
            icon={<LockOutlined />}
            onClick={() => handleResetPassword(record)}
          >
            重置密码
          </Button>
          <Button 
            type="link" 
            danger 
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const handleAdd = () => {
    setEditingUser(null);
    form.resetFields();
    setIsModalOpen(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    form.setFieldsValue(user);
    setIsModalOpen(true);
  };

  const handleResetPassword = (user: User) => {
    Modal.confirm({
      title: '确认重置密码',
      content: `确定要重置用户 ${user.username} 的密码吗？`,
      onOk: async () => {
        // TODO: 调用重置密码 API
        message.success('密码重置成功，新密码已发送至用户手机');
      },
    });
  };

  const handleDelete = (user: User) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除用户 ${user.username} 吗？此操作不可恢复。`,
      okType: 'danger',
      onOk: async () => {
        // TODO: 调用删除 API
        message.success('用户已删除');
      },
    });
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      console.log('用户信息:', values);
      // TODO: 调用创建/更新 API
      message.success(editingUser ? '用户更新成功' : '用户创建成功');
      setIsModalOpen(false);
      form.resetFields();
    } catch (error) {
      console.error('表单验证失败:', error);
    }
  };

  const handleCancel = () => {
    setIsModalOpen(false);
    form.resetFields();
  };

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2>用户管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增用户
        </Button>
      </div>

      <Table 
        columns={columns} 
        dataSource={[]} 
        rowKey="id"
        scroll={{ x: 1200 }}
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`,
        }}
      />

      <Modal
        title={editingUser ? '编辑用户' : '新增用户'}
        open={isModalOpen}
        onOk={handleOk}
        onCancel={handleCancel}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ status: 'ACTIVE', role: 'CUSTOMER' }}
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, max: 20, message: '用户名长度为3-20个字符' },
            ]}
          >
            <Input placeholder="请输入用户名" disabled={!!editingUser} />
          </Form.Item>

          <Form.Item
            label="昵称"
            name="nickname"
            rules={[{ max: 50, message: '昵称最多50个字符' }]}
          >
            <Input placeholder="请输入昵称" />
          </Form.Item>

          <Form.Item
            label="手机号"
            name="phone"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' },
            ]}
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              label="密码"
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码至少6个字符' },
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
            <Select>
              <Select.Option value="CUSTOMER">客户</Select.Option>
              <Select.Option value="DELIVERY">配送员</Select.Option>
              <Select.Option value="ADMIN">管理员</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="状态"
            name="status"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select>
              <Select.Option value="ACTIVE">正常</Select.Option>
              <Select.Option value="INACTIVE">未激活</Select.Option>
              <Select.Option value="BANNED">已禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Users;
