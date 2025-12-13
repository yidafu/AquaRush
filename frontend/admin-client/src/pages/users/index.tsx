import React, { useState } from 'react';
import { Tabs, Button, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { TabsProps } from 'antd';

// Import components
import AdminTable from './components/AdminTable';
import DeliveryWorkerTable from './components/DeliveryWorkerTable';
import UserTable from './components/UserTable';
import AdminForm from './components/AdminForm';
import DeliveryWorkerForm from './components/DeliveryWorkerForm';
import UserForm from './components/UserForm';
import AdminDetailModal from './components/AdminDetailModal';
import DeliveryWorkerDetailModal from './components/DeliveryWorkerDetailModal';
import UserDetailModal from './components/UserDetailModal';

// Import GraphQL hooks and types
import {
  useAdmins,
  useDeliveryWorkers,
  useUsers,
} from '../../services/user-graphql';
import type {
  Admin,
  DeliveryWorker,
  User,
} from '../../types/graphql';

// Import types for forms
import type { AdminFormData, DeliveryWorkerFormData, UserFormData } from './components/types';

const Users: React.FC = () => {
  // State for tabs and data
  const [activeTab, setActiveTab] = useState<string>('admin');
  const [loading, setLoading] = useState<boolean>(false);

  // GraphQL data fetching
  const { data: adminsData, loading: adminsLoading, refetch: refetchAdmins } = useAdmins();
  const { data: deliveryWorkersData, loading: deliveryWorkersLoading, refetch: refetchDeliveryWorkers } = useDeliveryWorkers();
  const { data: usersData, loading: usersLoading, refetch: refetchUsers } = useUsers();

  // State for modals
  const [adminFormVisible, setAdminFormVisible] = useState<boolean>(false);
  const [deliveryWorkerFormVisible, setDeliveryWorkerFormVisible] = useState<boolean>(false);
  const [userFormVisible, setUserFormVisible] = useState<boolean>(false);

  const [adminDetailVisible, setAdminDetailVisible] = useState<boolean>(false);
  const [deliveryWorkerDetailVisible, setDeliveryWorkerDetailVisible] = useState<boolean>(false);
  const [userDetailVisible, setUserDetailVisible] = useState<boolean>(false);

  // State for editing records (using mock data types temporarily)
  const [editingAdmin, setEditingAdmin] = useState<any>(null);
  const [editingDeliveryWorker, setEditingDeliveryWorker] = useState<any>(null);
  const [editingUser, setEditingUser] = useState<any>(null);

  // State for viewing details (using mock data types temporarily)
  const [viewingAdmin, setViewingAdmin] = useState<any>(null);
  const [viewingDeliveryWorker, setViewingDeliveryWorker] = useState<any>(null);
  const [viewingUser, setViewingUser] = useState<any>(null);

  // Use GraphQL data only
  const admins = adminsData?.admins || [];
  const deliveryWorkers = deliveryWorkersData?.deliveryWorkers || [];
  const users = usersData?.users?.content || [];

  // Common action handlers
  const handleViewAdmin = (admin: Admin) => {
    setViewingAdmin(admin);
    setAdminDetailVisible(true);
  };

  const handleViewDeliveryWorker = (deliveryWorker: DeliveryWorker) => {
    setViewingDeliveryWorker(deliveryWorker);
    setDeliveryWorkerDetailVisible(true);
  };

  const handleViewUser = (user: User) => {
    setViewingUser(user);
    setUserDetailVisible(true);
  };

  const handleEditAdmin = (admin: Admin) => {
    setEditingAdmin(admin);
    setAdminFormVisible(true);
  };

  const handleEditDeliveryWorker = (deliveryWorker: DeliveryWorker) => {
    setEditingDeliveryWorker(deliveryWorker);
    setDeliveryWorkerFormVisible(true);
  };

  const handleEditUser = (user: User) => {
    setEditingUser(user);
    setUserFormVisible(true);
  };

  const handleDeleteAdmin = (admin: Admin) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除管理员 ${admin.username || admin.realName} 吗？此操作不可恢复。`,
      okType: 'danger',
      onOk: async () => {
        try {
          // TODO: Call delete API
          await new Promise(resolve => setTimeout(resolve, 1000));
          // Use refetch to update data from GraphQL
          refetchUsers();
          message.success('管理员删除成功');
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  const handleDeleteDeliveryWorker = (deliveryWorker: DeliveryWorker) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除送水员 ${deliveryWorker.name} 吗？此操作不可恢复。`,
      okType: 'danger',
      onOk: async () => {
        setLoading(true);
        try {
          // TODO: Call delete API
          await new Promise(resolve => setTimeout(resolve, 1000));
          await refetchDeliveryWorkers();
          message.success('送水员删除成功');
        } catch (error) {
          message.error('删除失败');
        } finally {
          setLoading(false);
        }
      },
    });
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

  const handleResetPassword = (record: any) => {
    const name = record.username || record.name || record.nickname || record.wechatOpenId;
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

  // Form submission handlers
  const handleAdminSubmit = async (values: AdminFormData) => {
    setLoading(true);
    try {
      // TODO: Call create/update API
      await new Promise(resolve => setTimeout(resolve, 1000));

      if (editingAdmin) {
        message.success('管理员更新成功');
      } else {
        message.success('管理员创建成功');
      }

      await refetchAdmins();
      setAdminFormVisible(false);
      setEditingAdmin(null);
    } catch (error) {
      message.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  const handleDeliveryWorkerSubmit = async (values: DeliveryWorkerFormData) => {
    setLoading(true);
    try {
      // TODO: Call create/update API with values
      await new Promise(resolve => setTimeout(resolve, 1000));

      if (editingDeliveryWorker) {
        message.success('送水员更新成功');
      } else {
        message.success('送水员创建成功');
      }

      await refetchDeliveryWorkers();
      setDeliveryWorkerFormVisible(false);
      setEditingDeliveryWorker(null);
    } catch (error) {
      message.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  const handleUserSubmit = async (values: UserFormData) => {
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

  // Tab items configuration
  const tabItems: TabsProps['items'] = [
    {
      key: 'admin',
      label: (
        <span>
          管理员
          <span style={{ marginLeft: 8, color: '#999', fontSize: 12 }}>
            ({admins.length})
          </span>
        </span>
      ),
      children: (
        <div>
          <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ margin: 0, fontSize: 20 }}>管理员管理</h3>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => {
              setEditingAdmin(null);
              setAdminFormVisible(true);
            }}>
              新增管理员
            </Button>
          </div>
          <AdminTable
            data={admins}
            loading={adminsLoading}
            onEdit={handleEditAdmin}
            onView={handleViewAdmin}
            onDelete={handleDeleteAdmin}
            onResetPassword={handleResetPassword}
          />
        </div>
      ),
    },
    {
      key: 'delivery-worker',
      label: (
        <span>
          送水员
          <span style={{ marginLeft: 8, color: '#999', fontSize: 12 }}>
            ({deliveryWorkers.length})
          </span>
        </span>
      ),
      children: (
        <div>
          <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ margin: 0, fontSize: 20 }}>送水员管理</h3>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => {
              setEditingDeliveryWorker(null);
              setDeliveryWorkerFormVisible(true);
            }}>
              新增送水员
            </Button>
          </div>
          <DeliveryWorkerTable
            data={deliveryWorkers}
            loading={deliveryWorkersLoading}
            onEdit={handleEditDeliveryWorker}
            onView={handleViewDeliveryWorker}
            onDelete={handleDeleteDeliveryWorker}
            onResetPassword={handleResetPassword}
          />
        </div>
      ),
    },
    {
      key: 'user',
      label: (
        <span>
          用户
          <span style={{ marginLeft: 8, color: '#999', fontSize: 12 }}>
            ({users.length})
          </span>
        </span>
      ),
      children: (
        <div>
          <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ margin: 0, fontSize: 20 }}>用户管理</h3>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => {
              setEditingUser(null);
              setUserFormVisible(true);
            }}>
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
        </div>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px', background: '#fff' }}>
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        size="large"
        items={tabItems}
        style={{ marginBottom: 32 }}
      />

      {/* Forms */}
      <AdminForm
        open={adminFormVisible}
        onClose={() => {
          setAdminFormVisible(false);
          setEditingAdmin(null);
        }}
        record={editingAdmin || undefined}
        onSubmit={handleAdminSubmit}
        loading={adminsLoading}
      />

      <DeliveryWorkerForm
        open={deliveryWorkerFormVisible}
        onClose={() => {
          setDeliveryWorkerFormVisible(false);
          setEditingDeliveryWorker(null);
        }}
        record={editingDeliveryWorker || undefined}
        onSubmit={handleDeliveryWorkerSubmit}
        loading={deliveryWorkersLoading}
      />

      <UserForm
        open={userFormVisible}
        onClose={() => {
          setUserFormVisible(false);
          setEditingUser(null);
        }}
        record={editingUser || undefined}
        onSubmit={handleUserSubmit}
        loading={usersLoading}
      />

      {/* Detail Modals */}
      <AdminDetailModal
        open={adminDetailVisible}
        onClose={() => {
          setAdminDetailVisible(false);
          setViewingAdmin(null);
        }}
        admin={viewingAdmin}
        loading={adminsLoading}
      />

      <DeliveryWorkerDetailModal
        open={deliveryWorkerDetailVisible}
        onClose={() => {
          setDeliveryWorkerDetailVisible(false);
          setViewingDeliveryWorker(null);
        }}
        deliveryWorker={viewingDeliveryWorker}
        addresses={[]}
        loading={deliveryWorkersLoading}
      />

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

export default Users;
