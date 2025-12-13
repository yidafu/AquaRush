import React from 'react';
import { Table, Button, Space, Tag, Avatar } from 'antd';
import { EditOutlined, LockOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import type { User, TableActionProps } from './types';
import { STATUS_MAPPINGS, ROLE_MAPPINGS } from './types';

interface UserTableProps extends TableActionProps {
  data: User[];
  loading?: boolean;
}

const UserTable: React.FC<UserTableProps> = ({
  data,
  loading = false,
  onEdit,
  onDelete,
  onResetPassword,
}) => {
  const navigate = useNavigate();
  const columns: ColumnsType<User> = [
    {
      title: '头像',
      dataIndex: 'avatarUrl',
      key: 'avatarUrl',
      width: 80,
      render: (avatarUrl: string, record: User) => (
        <Avatar
          src={avatarUrl}
          size={40}
          style={{ backgroundColor: '#1890ff' }}
        >
          {record.nickname?.charAt(0)?.toUpperCase() || 'U'}
        </Avatar>
      ),
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      width: 150,
      fixed: 'left',
      render: (nickname: string) => nickname || '未设置昵称',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
      render: (phone: string) => phone || '-',
    },
    {
      title: '微信OpenID',
      dataIndex: 'wechatOpenId',
      key: 'wechatOpenId',
      width: 200,
      render: (wechatOpenId: string) => (
        <span title={wechatOpenId}>
          {wechatOpenId ? `${wechatOpenId.substring(0, 8)}...` : '-'}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const config = STATUS_MAPPINGS[status as keyof typeof STATUS_MAPPINGS];
        if (!config) {
          return <Tag color="default">未知状态</Tag>;
        }
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 100,
      render: (role: string) => {
        const config = ROLE_MAPPINGS[role as keyof typeof ROLE_MAPPINGS];
        if (!config) {
          return <Tag color="default">未知角色</Tag>;
        }
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '注册时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (createdAt: string) => new Date(createdAt).toLocaleString('zh-CN'),
    },
    {
      title: '最后更新',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 160,
      render: (updatedAt: string) => new Date(updatedAt).toLocaleString('zh-CN'),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/users/${record.id}`)}
            size="small"
          >
            查看
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => onEdit(record)}
            size="small"
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<LockOutlined />}
            onClick={() => onResetPassword(record)}
            size="small"
          >
            重置密码
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => onDelete(record)}
            size="small"
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={data}
      rowKey="id"
      loading={loading}
      scroll={{ x: 1200 }}
      pagination={{
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (total, range) =>
          `第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
        pageSizeOptions: ['10', '20', '50', '100'],
        defaultPageSize: 20,
      }}
      size="middle"
    />
  );
};

export default UserTable;