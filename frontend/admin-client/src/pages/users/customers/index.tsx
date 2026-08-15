import React, { useState, useEffect } from 'react';
import { Button, Input, Modal, Select, Space, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

// Import components
import UserTable from '../components/UserTable';
import UserForm from '../components/UserForm';

// Import GraphQL hooks and types
import {
  useUsers,
  useCreateUser,
  useUpdateUser,
  useDeleteUser,
  type CreateUserInput,
  type UpdateUserInput,
} from '../../../services/user-graphql';
import {
  useCustomerGroups,
  useSetUserCustomerGroup,
} from '../../../services/marketing-graphql';
import type { User } from '@aquarush/common';
import { UserStatus } from '@aquarush/common';

// Import types for forms
import type { UserFormData } from '../components/types';
import { STATUS_MAPPINGS } from '../components/types';

// 未分组选项占位值(提交时转换为 null)
const NO_GROUP_VALUE = '__NONE__';

/** 客户分组选项最小形状(marketing-graphql 返回类型为 any,这里显式标注) */
interface CustomerGroupOption {
  id: string | number;
  name: string;
  memberCount?: number;
}

const CustomerUsers: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [groupFilter, setGroupFilter] = useState<string | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<UserStatus | undefined>(undefined);
  const [search, setSearch] = useState<string | undefined>(undefined);

  // GraphQL data fetching(search 为关键字搜索,与分组/状态筛选可组合;F-USR-022)
  const { data: usersData, loading: usersLoading, refetch: refetchUsers } = useUsers(
    (() => {
      const input: { customerGroupId?: string; search?: string; status?: UserStatus } = {};
      if (groupFilter) input.customerGroupId = groupFilter;
      if (statusFilter) input.status = statusFilter;
      if (search) input.search = search;
      return Object.keys(input).length > 0 ? input : undefined;
    })(),
  );

  // 客户分组(筛选器 + 设置分组弹窗共用)
  const [loadGroups, { data: groupsData }] = useCustomerGroups();
  const [setUserGroup, { loading: settingGroup }] = useSetUserCustomerGroup();

  // 用户增改删(G11 落地,F-USR-022)
  const [createUser] = useCreateUser();
  const [updateUser] = useUpdateUser();
  const [deleteUser] = useDeleteUser();

  useEffect(() => {
    loadGroups({ variables: { input: { page: 0, size: 200 } } });
  }, [loadGroups]);

  // State for modals
  const [userFormVisible, setUserFormVisible] = useState<boolean>(false);

  // State for editing records
  const [editingUser, setEditingUser] = useState<User | null>(null);

  // 设置分组弹窗状态
  const [groupTarget, setGroupTarget] = useState<User | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<string | undefined>(undefined);

  // Use GraphQL data only
  const users = usersData?.users?.list || [];
  const groups = (groupsData?.customerGroups?.content ?? []) as CustomerGroupOption[];

  const handleGroupFilterChange = (value: string | undefined) => {
    setGroupFilter(value === NO_GROUP_VALUE ? undefined : value);
  };

  const handleSearch = (value: string) => {
    setSearch(value.trim() || undefined);
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
        try {
          const res = await deleteUser({ variables: { id: user.id } });
          if (res.data?.deleteUser) {
            message.success('用户删除成功');
          } else {
            message.warning('该用户不存在或已删除');
          }
          await refetchUsers();
        } catch (error) {
          message.error(error instanceof Error ? error.message : '删除失败');
        }
      },
    });
  };

  // Form submission handler(G11 落地:真实调用 createUser/updateUser)
  const handleUserSubmit = async (values: UserFormData) => {
    setLoading(true);
    try {
      if (editingUser && editingUser.id) {
        const input: UpdateUserInput = {
          nickname: values.nickname,
          phone: values.phone,
          avatarUrl: values.avatarUrl,
          status: values.status,
        };
        await updateUser({
          variables: {
            id: editingUser.id,
            input,
          },
        });
        message.success('用户更新成功');
      } else {
        const input: CreateUserInput = {
          nickname: values.nickname,
          phone: values.phone,
          avatarUrl: values.avatarUrl,
        };
        await createUser({
          variables: {
            input,
          },
        });
        message.success('用户创建成功');
      }

      await refetchUsers();
      setUserFormVisible(false);
      setEditingUser(null);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          <h3 style={{ margin: 0 }}>用户列表</h3>
          <Input.Search
            placeholder="搜索昵称/手机号"
            allowClear
            style={{ width: 220 }}
            onSearch={handleSearch}
          />
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
          <Select
            allowClear
            placeholder="筛选账户状态"
            style={{ width: 160 }}
            value={statusFilter}
            onChange={(value: UserStatus | undefined) => setStatusFilter(value)}
            options={(
              ['ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'] as const
            ).map((status) => ({
              value: status,
              label: STATUS_MAPPINGS[status].text,
            }))}
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
        onDelete={handleDeleteUser}
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
        record={editingUser ? {
          nickname: editingUser.nickname ?? undefined,
          phone: editingUser.phone ?? undefined,
          avatarUrl: editingUser.avatarUrl ?? undefined,
          status: editingUser.status as UserFormData['status'],
        } : undefined}
        onSubmit={handleUserSubmit}
        loading={loading}
      />
    </div>
  );
};

export default CustomerUsers;
