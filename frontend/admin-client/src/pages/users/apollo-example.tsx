import React, { useState } from 'react';
import { Card, Table, Button, Space, Input, Select, message, Modal } from 'antd';
import { SearchOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

// Import GraphQL hooks
import {
  useUsers,
  useDeleteUser,
  useToggleUserStatus,
  useLazyUsers,
} from '../../services/user-graphql';
import type {
  User,
  UserListInput,
  UserStatus,
} from '../../types/graphql';

const ApolloExample: React.FC = () => {
  // State for pagination and filters
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });
  const [filters, setFilters] = useState({
    keyword: '',
    status: '',
  });

  // GraphQL hooks
  const { data, loading, error, refetch } = useUsers({
    page: pagination.current - 1,
    size: pagination.pageSize,
    keyword: filters.keyword,
    status: filters.status,
  });

  const [deleteUser] = useDeleteUser();
  const [toggleUserStatus] = useToggleUserStatus();
  const [searchUsers, { loading: searchLoading }] = useLazyUsers();

  // Handle delete user
  const handleDelete = (user: User) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除用户 ${user.nickname || user.wechatOpenId} 吗？`,
      okType: 'danger',
      onOk: async () => {
        try {
          await deleteUser({ variables: { id: user.id } });
          // Data will be automatically refetched due to refetchQueries in the hook
        } catch (err) {
          console.error('Delete user error:', err);
        }
      },
    });
  };

  // Handle toggle user status
  const handleToggleStatus = async (user: User) => {
    const newStatus: UserStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await toggleUserStatus({
        variables: {
          id: user.id,
          status: newStatus
        }
      });
    } catch (err) {
      console.error('Toggle user status error:', err);
    }
  };

  // Handle search
  const handleSearch = () => {
    setPagination({ ...pagination, current: 1 });
    refetch({
      page: 0,
      size: pagination.pageSize,
      keyword: filters.keyword,
      status: filters.status,
    });
  };

  // Handle table change
  const handleTableChange = (newPagination: any) => {
    setPagination({
      current: newPagination.current,
      pageSize: newPagination.pageSize,
    });
  };

  // Table columns
  const columns: ColumnsType<User> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      render: (text: string) => text || '-',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      render: (text: string) => text || '-',
    },
    {
      title: '头像',
      dataIndex: 'avatarUrl',
      key: 'avatarUrl',
      width: 80,
      render: (url: string) => url ?
        <img src={url} alt="avatar" style={{ width: 40, height: 40, borderRadius: '50%' }} /> :
        '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: UserStatus) => {
        const statusMap = {
          ACTIVE: { text: '活跃', color: 'green' },
          INACTIVE: { text: '非活跃', color: 'orange' },
          BANNED: { text: '已禁用', color: 'red' },
        };
        const statusInfo = statusMap[status];
        return (
          <span style={{ color: statusInfo.color }}>
            {statusInfo.text}
          </span>
        );
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (text: string) => new Date(text).toLocaleString(),
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
      render: (text: string) => new Date(text).toLocaleString(),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />}>
            编辑
          </Button>
          <Button
            size="small"
            type={record.status === 'ACTIVE' ? 'default' : 'primary'}
            onClick={() => handleToggleStatus(record)}
          >
            {record.status === 'ACTIVE' ? '禁用' : '启用'}
          </Button>
          <Button
            size="small"
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

  // Error display
  if (error) {
    return (
      <Card title="用户管理" style={{ margin: 24 }}>
        <div style={{ textAlign: 'center', padding: 40 }}>
          <p style={{ color: 'red' }}>加载失败: {error.message}</p>
          <Button type="primary" onClick={() => refetch()}>
            重试
          </Button>
        </div>
      </Card>
    );
  }

  return (
    <Card title="Apollo Client 示例 - 用户管理" style={{ margin: 24 }}>
      {/* Search and filters */}
      <Space style={{ marginBottom: 16 }}>
        <Input
          placeholder="搜索用户"
          prefix={<SearchOutlined />}
          value={filters.keyword}
          onChange={(e) => setFilters({ ...filters, keyword: e.target.value })}
          onPressEnter={handleSearch}
          style={{ width: 200 }}
        />
        <Select
          placeholder="用户状态"
          value={filters.status}
          onChange={(value) => setFilters({ ...filters, status: value || '' })}
          style={{ width: 120 }}
          allowClear
        >
          <Select.Option value="ACTIVE">活跃</Select.Option>
          <Select.Option value="INACTIVE">非活跃</Select.Option>
          <Select.Option value="BANNED">已禁用</Select.Option>
        </Select>
        <Button
          type="primary"
          icon={<SearchOutlined />}
          onClick={handleSearch}
          loading={searchLoading}
        >
          搜索
        </Button>
        <Button type="primary" icon={<PlusOutlined />}>
          新增用户
        </Button>
      </Space>

      {/* Data table */}
      <Table
        columns={columns}
        dataSource={data?.users?.content || []}
        loading={loading}
        rowKey="id"
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: data?.users?.totalElements,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`,
          onChange: (page, pageSize) => {
            setPagination({ current: page, pageSize: pageSize || 10 });
          },
        }}
        onChange={handleTableChange}
        scroll={{ x: 1200 }}
      />

      {/* Info section */}
      <div style={{ marginTop: 24, padding: 16, backgroundColor: '#f5f5f5', borderRadius: 4 }}>
        <h4>Apollo Client 集成说明：</h4>
        <ul>
          <li>✅ 使用 <code>useUsers</code> hook 自动获取和管理用户数据</li>
          <li>✅ 自动处理 loading 状态和错误信息</li>
          <li>✅ 删除和状态更新操作后自动刷新列表（通过 refetchQueries）</li>
          <li>✅ 支持分页和搜索参数</li>
          <li>✅ 类型安全 - 使用生成的 TypeScript 类型</li>
        </ul>
      </div>
    </Card>
  );
};

export default ApolloExample;