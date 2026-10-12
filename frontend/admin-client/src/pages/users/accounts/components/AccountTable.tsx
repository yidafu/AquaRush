import React from 'react';
import { Table, Space, Tag, Avatar, Button } from '@/ui';
import {
  EditOutlined,
  LockOutlined,
  DeleteOutlined,
  EyeOutlined,
} from '@/ui/icons';
import { avatarFromName } from '@aquarush/common/utils';
import type { ColumnsType } from '@/ui';
import type { AccountRow } from '../types';
import { ROLE_MAPPINGS, WORKER_STATUS_MAPPINGS } from '../../components/types';

export interface AccountTableProps {
  accounts: AccountRow[];
  loading: boolean;
  /** 删除/重置密码可用性(普通管理员仅 DELIVERY_WORKER 行可用,后端强制) */
  canDeleteOrReset: (record: AccountRow) => boolean;
  onView: (record: AccountRow) => void;
  onEdit: (record: AccountRow) => void;
  onDelete: (record: AccountRow) => void;
  onReset: (record: AccountRow) => void;
  /** 服务端分页(规则 10:列表接口分页,表格不再自行切页) */
  pagination: {
    total: number;
    page: number;
    size: number;
    onChange: (page: number, size: number) => void;
  };
}

/** 账号列表表格(users/accounts/index.tsx 拆分):列定义 + 行操作 */
const AccountTable: React.FC<AccountTableProps> = ({
  accounts,
  loading,
  canDeleteOrReset,
  onView,
  onEdit,
  onDelete,
  onReset,
  pagination,
}) => {
  const columns: ColumnsType<AccountRow> = [
    {
      title: '头像',
      dataIndex: 'avatarUrl',
      key: 'avatarUrl',
      width: 70,
      render: (avatarUrl: string, record: AccountRow) => (
        <Avatar
          src={avatarUrl || avatarFromName(record.username || record.name || record.realName || '账号')}
          size="small"
        />
      ),
    },
    {
      title: '账号',
      dataIndex: 'username',
      key: 'username',
      width: 170,
      render: (username: string, record: AccountRow) => username || record.name || '-',
    },
    {
      title: '姓名',
      dataIndex: 'realName',
      key: 'realName',
      width: 120,
      render: (realName: string, record: AccountRow) => realName || (record.role === 'DELIVERY_WORKER' ? record.name || '-' : '-'),
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
      render: (phone: string) => phone || '-',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 120,
      render: (role: AccountRow['role']) => {
        const config = ROLE_MAPPINGS[role as keyof typeof ROLE_MAPPINGS];
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '在线状态',
      dataIndex: 'onlineStatus',
      key: 'onlineStatus',
      width: 100,
      render: (onlineStatus: string, record: AccountRow) => {
        if (record.role !== 'DELIVERY_WORKER') return '-';
        const config = WORKER_STATUS_MAPPINGS[onlineStatus as keyof typeof WORKER_STATUS_MAPPINGS];
        return config ? <Tag color={config.color}>{config.text}</Tag> : '-';
      },
    },
    {
      title: '最后登录时间',
      dataIndex: 'lastLoginAt',
      key: 'lastLoginAt',
      width: 160,
      render: (lastLoginAt: string) => {
        if (!lastLoginAt) return '从未登录';
        return new Date(lastLoginAt).toLocaleString('zh-CN');
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (createdAt: string) => new Date(createdAt).toLocaleString('zh-CN'),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" icon={<EyeOutlined />} onClick={() => onView(record)} size="small">
            查看
          </Button>
          <Button type="link" icon={<EditOutlined />} onClick={() => onEdit(record)} size="small">
            编辑
          </Button>
          {canDeleteOrReset(record) && (
            <>
              <Button
                type="link"
                icon={<LockOutlined />}
                onClick={() => onReset(record)}
                size="small"
              >
                重置密码
              </Button>
              <Button type="link" danger icon={<DeleteOutlined />} onClick={() => onDelete(record)} size="small">
                删除
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={accounts}
      loading={loading}
      rowKey="id"
      scroll={{ x: 1200 }}
      pagination={{
        current: pagination.page + 1,
        pageSize: pagination.size,
        total: pagination.total,
        showSizeChanger: true,
        showTotal: (total) => `共 ${total} 条`,
        onChange: (page, size) => pagination.onChange(page - 1, size),
      }}
    />
  );
};

export default AccountTable;
