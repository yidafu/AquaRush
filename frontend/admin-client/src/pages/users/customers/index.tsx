import React, { useState } from 'react';
import { Modal, message } from '@/ui';

// Import components
import UserTable from '../components/UserTable';
import UserForm from '../components/UserForm';
import { CustomerToolbar, SetCustomerGroupModal } from './components';

// Import GraphQL hooks and types
import { useQuery } from '@apollo/client';
import {
  CUSTOMER_USERS_PAGE_DATA_QUERY,
} from '../../../graphql/queries/user.graphql';
import {
  useCreateUser,
  useUpdateUser,
  useDeleteUser,
  type CreateUserInput,
  type UpdateUserInput,
} from '../../../services/user-graphql';
import {
  useSetUserCustomerGroup,
} from '../../../services/marketing-graphql';
import { UserStatus } from '@aquarush/common';

// Import types for forms
import type { User as CustomerUser, UserFormData } from '../components/types';
import type { CustomerGroupOption } from './components';

/** 客户列表页单页查询(CustomerUsersPageData)返回结构 */
interface CustomerUsersPageData {
  users?: {
    list: CustomerUser[];
    pageInfo?: {
      total?: number;
      pageSize?: number;
      pageNum?: number;
      hasNext?: boolean;
      hasPrevious?: boolean;
      totalPages?: number;
    };
  };
  customerGroups?: {
    content: CustomerGroupOption[];
  };
}

const CustomerUsers: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [groupFilter, setGroupFilter] = useState<string | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<UserStatus | undefined>(undefined);
  const [search, setSearch] = useState<string | undefined>(undefined);

  // 单页单请求:用户分页 + 客户分组(筛选/设置分组弹窗)一次取回(规则 #10);
  // 筛选/搜索变化经 variables 变化自动重取(search 为关键字搜索,与分组/状态筛选可组合;F-USR-022)
  const { data, loading: usersLoading, refetch } = useQuery<CustomerUsersPageData>(CUSTOMER_USERS_PAGE_DATA_QUERY, {
    variables: {
      input: (() => {
        const input: { customerGroupId?: string; search?: string; status?: UserStatus } = {};
        if (groupFilter) input.customerGroupId = groupFilter;
        if (statusFilter) input.status = statusFilter;
        if (search) input.search = search;
        return Object.keys(input).length > 0 ? input : undefined;
      })(),
    },
    errorPolicy: 'all',
  });

  // 设置分组(弹窗提交)
  const [setUserGroup, { loading: settingGroup }] = useSetUserCustomerGroup();

  // 用户增改删(G11 落地,F-USR-022)
  const [createUser] = useCreateUser();
  const [updateUser] = useUpdateUser();
  const [deleteUser] = useDeleteUser();

  // State for modals
  const [userFormVisible, setUserFormVisible] = useState<boolean>(false);

  // State for editing records
  const [editingUser, setEditingUser] = useState<CustomerUser | null>(null);

  // 设置分组弹窗状态
  const [groupTarget, setGroupTarget] = useState<CustomerUser | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<string | undefined>(undefined);

  // Use GraphQL data only
  const users = data?.users?.list || [];
  const groups = (data?.customerGroups?.content ?? []) as CustomerGroupOption[];

  const handleGroupFilterChange = (value: string | undefined) => {
    setGroupFilter(value === '__NONE__' ? undefined : value);
  };

  const handleSearch = (value: string) => {
    setSearch(value.trim() || undefined);
  };

  const handleSetGroup = (user: CustomerUser) => {
    setGroupTarget(user);
    setSelectedGroup(user.customerGroupId ?? undefined);
  };

  const handleConfirmSetGroup = async () => {
    if (!groupTarget) return;
    try {
      await setUserGroup({
        variables: {
          userId: groupTarget.id,
          customerGroupId: !selectedGroup || selectedGroup === '__NONE__' ? null : selectedGroup,
        },
      });
      setGroupTarget(null);
    } catch {
      // 错误提示已由 hook 的 onError 处理
    }
  };

  // Action handlers
  const handleEditUser = (user: CustomerUser) => {
    setEditingUser(user);
    setUserFormVisible(true);
  };

  const handleDeleteUser = (user: CustomerUser) => {
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
          await refetch();
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

      await refetch();
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
      <CustomerToolbar
        groupFilter={groupFilter}
        statusFilter={statusFilter}
        groups={groups}
        onGroupFilterChange={handleGroupFilterChange}
        onStatusFilterChange={setStatusFilter}
        onSearch={handleSearch}
        onAdd={() => {
          setEditingUser(null);
          setUserFormVisible(true);
        }}
      />

      <UserTable
        data={users}
        loading={usersLoading}
        onEdit={handleEditUser}
        onDelete={handleDeleteUser}
        onSetGroup={handleSetGroup}
      />

      {/* 设置分组 Modal */}
      <SetCustomerGroupModal
        target={groupTarget}
        groups={groups}
        selectedGroup={selectedGroup}
        confirmLoading={settingGroup}
        onSelectedGroupChange={setSelectedGroup}
        onOk={handleConfirmSetGroup}
        onCancel={() => setGroupTarget(null)}
      />

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
