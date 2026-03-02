import React, { useState } from 'react';
import { Button, Modal, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

// Import components
import AdminTable from '../components/AdminTable';
import AdminForm from '../components/AdminForm';
import AdminDetailModal from '../components/AdminDetailModal';

// Import GraphQL hooks and types
import {
  useAdmins,
} from '../../../services/user-graphql';
import type { Admin } from '@aquarush/common';

// Import types for forms
import type { AdminFormData } from '../components/types';

const AdminUsers: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);

  // GraphQL data fetching
  const { data: adminsData, loading: adminsLoading, refetch: refetchAdmins } = useAdmins();

  // State for modals
  const [adminFormVisible, setAdminFormVisible] = useState<boolean>(false);
  const [adminDetailVisible, setAdminDetailVisible] = useState<boolean>(false);

  // State for editing/viewing records
  const [editingAdmin, setEditingAdmin] = useState<Admin | null>(null);
  const [viewingAdmin, setViewingAdmin] = useState<Admin | null>(null);

  // Use GraphQL data only
  const admins = adminsData?.admins || [];

  // Action handlers
  const handleViewAdmin = (admin: Admin) => {
    setViewingAdmin(admin);
    setAdminDetailVisible(true);
  };

  const handleEditAdmin = (admin: Admin) => {
    setEditingAdmin(admin);
    setAdminFormVisible(true);
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
          message.success('管理员删除成功');
          refetchAdmins();
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  const handleResetPassword = (record: Admin) => {
    const name = record.username || record.realName || '';
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
  const handleAdminSubmit = async (_values: AdminFormData) => {
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

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0 }}>管理员列表</h3>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditingAdmin(null);
            setAdminFormVisible(true);
          }}
        >
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

      {/* Form Modal */}
      <AdminForm
        open={adminFormVisible}
        onClose={() => {
          setAdminFormVisible(false);
          setEditingAdmin(null);
        }}
        record={editingAdmin || undefined}
        onSubmit={handleAdminSubmit}
        loading={loading}
      />

      {/* Detail Modal */}
      <AdminDetailModal
        open={adminDetailVisible}
        onClose={() => {
          setAdminDetailVisible(false);
          setViewingAdmin(null);
        }}
        admin={viewingAdmin}
        loading={adminsLoading}
      />
    </div>
  );
};

export default AdminUsers;
