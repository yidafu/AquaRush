import React, { useState, useEffect } from 'react';
import { Button, Modal, Select, Space, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

// Import components
import UserTable from '../components/UserTable';
import UserForm from '../components/UserForm';
import UserDetailModal from '../components/UserDetailModal';

// Import GraphQL hooks and types
import {
  useUsers,
} from '../../../services/user-graphql';
import {
  useCustomerGroups,
  useSetUserCustomerGroup,
} from '../../../services/marketing-graphql';
import type { User } from '@aquarush/common';

// Import types for forms
import type { UserFormData } from '../components/types';

// 未分组选项占位值(提交时转换为 null)
const NO_GROUP_VALUE = '__NONE__';

const CustomerUsers: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [groupFilter, setGroupFilter] = useState<string | undefined>(undefined);

  // GraphQL data fetching(customerGroupId 为可选筛选)
  const { data: usersData, loading: usersLoading, refetch: refetchUsers } = useUsers(
    groupFilter ? { customerGroupId: groupFilter } : undefined,
  );

  // 客户分组(筛选器 + 设置分组弹窗共用)
  const [loadGroups, { data: groupsData }] = useCustomerGroups();
  const [setUserGroup, { loading: settingGroup }] = useSetUserCustomerGroup();

  useEffect(() => {
    loadGroups({ variables: { input: { page: 0, size: 200 } } });
  }, [loadGroups]);

  // State for modals
  const [userFormVisible, setUserFormVisible] = useState<boolean>(false);
  const [userDetailVisible, setUserDetailVisible] = useState<boolean>(false);

  // State for editing/viewing records
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [viewingUser, setViewingUser] = useState<User | null>(null);

  // 设置分组弹窗状态
  const [groupTarget, setGroupTarget] = useState<User | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<string | undefined>(undefined);

  // Use GraphQL data only
  const users = usersData?.users?.list || [];
  const groups = groupsData?.customerGroups?.content ?? [];

  const handleGroupFilterChange = (value: string | undefined) => {
    setGroupFilter(value === NO_GROUP_VALUE ? undefined : value);
  };

  const handleSetGroup = (user: User) => {
    setGroupTarget(user);
    setSelectedGroup(user.customerGroupId ?? undefined);
  };

  const handleConfirmSetGroup = async () => {
    if (!groupTarget) return;
    try {
      await setUserGroup({
        variables: {
          userId: groupTarget.id,
          customerGroupId: !selectedGroup || selectedGroup === NO_GROUP_VALUE ? null : selectedGroup,
        },
      });
      setGroupTarget(null);
    } catch {
      // 错误提示已由 hook 的 onError 处理
    }
  };

  // Action handlers
  const handleViewUser = (user: User) => {
    setViewingUser(user);
    setUserDetailVisible(true);
  };

  const handleEditUser = (user: User) => {
    setEditingUser(user);
    setUserFormVisible(true);
  };

  const handleDeleteUser = (user: User) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除用户 ${user.nickname || user.wechatOpenId} 吗？此操作不可恢复。`,
      okType: 'danger',
      onOk: async () => {
        setLoading(true);
        try {
          // TODO: Call delete API
          await new Promise(resolve => setTimeout(resolve, 1000));
          await refetchUsers();
          message.success('用户删除成功');
        } catch (error) {
          message.error('删除失败');
        } finally {
          setLoading(false);
        }
      },
    });
  };

  const handleResetPassword = (record: User) => {
    const name = record.nickname || record.wechatOpenId || '';
    Modal.confirm({
      title: '确认重置密码',
      content: `确定要重置 ${name} 的密码吗？`,
      onOk: async () => {
        setLoading(true);
        try {
          // TODO: Call reset password API
          await new Promise(resolve => setTimeout(resolve, 1000));
          message.success('密码重置成功，新密码已发送至用户手机');
        } catch (error) {
          message.error('重置密码失败');
        } finally {
          setLoading(false);
        }
      },
    });
  };

  // Form submission handler
  const handleUserSubmit = async (_values: UserFormData) => {
    setLoading(true);
    try {
      // TODO: Call create/update API with values
      await new Promise(resolve => setTimeout(resolve, 1000));

      if (editingUser) {
        message.success('用户更新成功');
      } else {
        message.success('用户创建成功');
      }

      await refetchUsers();
      setUserFormVisible(false);
      setEditingUser(null);
    } catch (error) {
      message.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          <h3 style={{ margin: 0 }}>用户列表</h3>
          <Select
            allowClear
            placeholder="筛选客户分组"
            style={{ width: 200 }}
            value={groupFilter}
            onChange={handleGroupFilterChange}
            showSearch
            optionFilterProp="label"
            options={[
              { value: NO_GROUP_VALUE, label: '未分组' },
              ...groups.map((g) => ({ value: String(g.id), label: `${g.name}(${g.memberCount}人)` })),
            ]}
          />
        </Space>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditingUser(null);
            setUserFormVisible(true);
          }}
        >
          新增用户
        </Button>
      </div>

      <UserTable
        data={users}
        loading={usersLoading}
        onEdit={handleEditUser}
        onView={handleViewUser}
        onDelete={handleDeleteUser}
        onResetPassword={handleResetPassword}
        onSetGroup={handleSetGroup}
      />

      {/* 设置分组 Modal */}
      <Modal
        title={`设置分组 - ${groupTarget?.nickname || groupTarget?.wechatOpenId || ''}`}
        open={groupTarget !== null}
        onOk={handleConfirmSetGroup}
        onCancel={() => setGroupTarget(null)}
        confirmLoading={settingGroup}
        destroyOnClose
        okText="确定"
      >
        <div style={{ marginBottom: 8, color: '#888' }}>
          设置用户所属客户分组,选择「未分组」将用户移出当前分组(即时生效)。
        </div>
        <Select
          style={{ width: '100%' }}
          value={selectedGroup}
          onChange={setSelectedGroup}
          showSearch
          optionFilterProp="label"
          options={[
            { value: NO_GROUP_VALUE, label: '未分组' },
            ...groups.map((g) => ({ value: String(g.id), label: g.name })),
          ]}
          placeholder="选择客户分组"
        />
      </Modal>

      {/* Form Modal */}
      <UserForm
        open={userFormVisible}
        onClose={() => {
          setUserFormVisible(false);
          setEditingUser(null);
        }}
        record={editingUser || undefined}
        onSubmit={handleUserSubmit}
        loading={loading}
      />

      {/* Detail Modal */}
      <UserDetailModal
        open={userDetailVisible}
        onClose={() => {
          setUserDetailVisible(false);
          setViewingUser(null);
        }}
        user={viewingUser}
        addresses={[]}
        loading={usersLoading}
      />
    </div>
  );
};

export default CustomerUsers;
