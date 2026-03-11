import React, { useState } from 'react';
import { Modal, message } from 'antd';

// Import components
import DeliveryWorkerTable from '../components/DeliveryWorkerTable';
import DeliveryWorkerForm from '../components/DeliveryWorkerForm';
import DeliveryWorkerDetailModal from '../components/DeliveryWorkerDetailModal';

// Import GraphQL hooks and types
import {
  useDeliveryWorkers,
  useCreateDeliveryWorker,
  useUpdateDeliveryWorker,
  type CreateDeliveryWorkerInput,
} from '../../../services/user-graphql';
import type { DeliveryWorker } from '@aquarush/common';

// Import types for forms
import type { DeliveryWorkerFormData } from '../components/types';

const DeliveryWorkerUsers: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);

  // GraphQL data fetching
  const { data: deliveryWorkersData, loading: deliveryWorkersLoading, refetch: refetchDeliveryWorkers } = useDeliveryWorkers();

  // GraphQL mutations for delivery worker
  const [createDeliveryWorker, { loading: createLoading }] = useCreateDeliveryWorker();
  const [updateDeliveryWorker, { loading: updateLoading }] = useUpdateDeliveryWorker();

  // State for modals
  const [deliveryWorkerFormVisible, setDeliveryWorkerFormVisible] = useState<boolean>(false);
  const [deliveryWorkerDetailVisible, setDeliveryWorkerDetailVisible] = useState<boolean>(false);

  // State for editing/viewing records
  const [editingDeliveryWorker, setEditingDeliveryWorker] = useState<DeliveryWorker | null>(null);
  const [viewingDeliveryWorker, setViewingDeliveryWorker] = useState<DeliveryWorker | null>(null);

  // Use GraphQL data only
  const deliveryWorkers = deliveryWorkersData?.deliveryWorkers || [];

  // Action handlers
  const handleViewDeliveryWorker = (deliveryWorker: DeliveryWorker) => {
    setViewingDeliveryWorker(deliveryWorker);
    setDeliveryWorkerDetailVisible(true);
  };

  const handleEditDeliveryWorker = (deliveryWorker: DeliveryWorker) => {
    setEditingDeliveryWorker(deliveryWorker);
    setDeliveryWorkerFormVisible(true);
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

  const handleResetPassword = (record: DeliveryWorker) => {
    const name = record.name || '';
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
  const handleDeliveryWorkerSubmit = async (values: DeliveryWorkerFormData) => {
    setLoading(true);
    try {
      // Prepare input data - wechatOpenId can be empty, backend will generate one
      const input: CreateDeliveryWorkerInput = {
        name: values.name,
        phone: values.phone,
        password: values.password,
        avatarUrl: values.avatarUrl,
        wechatOpenId: values.wechatOpenId || undefined,
        isAvailable: values.isAvailable,
      };

      if (editingDeliveryWorker && editingDeliveryWorker.id) {
        // Update existing delivery worker
        await updateDeliveryWorker({
          variables: {
            workerId: editingDeliveryWorker.id,
            input,
          },
        });
      } else {
        // Create new delivery worker
        await createDeliveryWorker({
          variables: {
            input,
          },
        });
      }

      await refetchDeliveryWorkers();
      setDeliveryWorkerFormVisible(false);
      setEditingDeliveryWorker(null);
    } catch (error) {
      console.error('Delivery worker submit error:', error);
      message.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0 }}>送水员列表</h3>
        {/* 送水员现在通过管理员表单的"送水员"角色创建 */}
      </div>

      <DeliveryWorkerTable
        data={deliveryWorkers}
        loading={deliveryWorkersLoading}
        onEdit={handleEditDeliveryWorker}
        onView={handleViewDeliveryWorker}
        onDelete={handleDeleteDeliveryWorker}
        onResetPassword={handleResetPassword}
      />

      {/* Form Modal */}
      <DeliveryWorkerForm
        open={deliveryWorkerFormVisible}
        onClose={() => {
          setDeliveryWorkerFormVisible(false);
          setEditingDeliveryWorker(null);
        }}
        record={editingDeliveryWorker || undefined}
        onSubmit={handleDeliveryWorkerSubmit}
        loading={loading || createLoading || updateLoading}
      />

      {/* Detail Modal */}
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
    </div>
  );
};

export default DeliveryWorkerUsers;
