import React, { useState } from 'react';
import { Button, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

// Import components
import UserTable from '../components/UserTable';
import UserForm from '../components/UserForm';
import UserDetailModal from '../components/UserDetailModal';

// Import GraphQL hooks and types
import {
  useUsers,
} from '../../../services/user-graphql';
import type { User } from '@aquarush/common';

// Import types for forms
import type { UserFormData } from '../components/types';

const CustomerUsers: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);

  // GraphQL data fetching
  const { data: usersData, loading: usersLoading, refetch: refetchUsers } = useUsers();

  // State for modals
  const [userFormVisible, setUserFormVisible] = useState<boolean>(false);
  const [userDetailVisible, setUserDetailVisible] = useState<boolean>(false);

  // State for editing/viewing records
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [viewingUser, setViewingUser] = useState<User | null>(null);

  // Use GraphQL data only
  const users = usersData?.users?.content || [];

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
        <h3 style={{ margin: 0 }}>用户列表</h3>
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
      />

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
