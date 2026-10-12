import { useState } from 'react';
import { Modal, message } from '@/ui';
import { useNavigate } from 'react-router-dom';
import {
  useDeleteAdmin,
  useResetAdminPassword,
  useUpdateAdmin,
  useDeleteDeliveryWorker,
  useResetDeliveryWorkerPassword,
  useUpdateDeliveryWorker,
  type UpdateAdminInput,
  type UpdateDeliveryWorkerInput,
} from '../../../../../services/user-graphql';
import type {
  AdminFormData,
  DeliveryWorkerFormData,
} from '../../../components/types';
import type { DeliveryRecord } from '../types';

export interface UseAccountDetailActionsOptions {
  record: DeliveryRecord | undefined;
  isSuperAdmin: boolean;
  refetch: () => void;
}

export interface AccountDetailActions {
  onEdit: () => void;
  onResetPassword: () => void;
  onToggleStatus: () => void;
  onDelete: () => void;
}

/**
 * 账号详情页操作逻辑(users/accounts/detail/index.tsx 拆分):
 * 删除/重置密码/启停用 + 两个编辑表单提交,操作按权限矩阵分发:
 * 超管走 admin mutation,普通管理员走 worker mutation(R-AUTH-04 后端强制)。
 */
export const useAccountDetailActions = ({
  record,
  isSuperAdmin,
  refetch,
}: UseAccountDetailActionsOptions) => {
  const navigate = useNavigate();

  // Mutations
  const [deleteAdmin] = useDeleteAdmin();
  const [resetAdminPassword] = useResetAdminPassword();
  const [updateAdmin] = useUpdateAdmin();
  const [deleteWorker] = useDeleteDeliveryWorker();
  const [resetWorkerPassword] = useResetDeliveryWorkerPassword();
  const [updateWorker] = useUpdateDeliveryWorker();

  const [opLoading, setOpLoading] = useState<boolean>(false);
  const [resetOpen, setResetOpen] = useState<boolean>(false);
  const [adminFormVisible, setAdminFormVisible] = useState<boolean>(false);
  const [workerFormVisible, setWorkerFormVisible] = useState<boolean>(false);

  const handleDelete = () => {
    if (!record) return;
    const name = record.name || record.username || '';
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除${record.role === 'DELIVERY_WORKER' ? '送水员' : '账号'} ${name} 吗？此操作不可恢复。`,
      okType: 'danger',
      onOk: async () => {
        setOpLoading(true);
        try {
          if (isSuperAdmin) {
            await deleteAdmin({ variables: { id: record.id } });
          } else {
            await deleteWorker({ variables: { id: record.id } });
          }
          message.success('删除成功');
          navigate('/users/accounts');
        } catch {
          // 错误提示已由 hook onError 处理
        } finally {
          setOpLoading(false);
        }
      },
    });
  };

  const handleResetPassword = async (newPassword?: string) => {
    if (!record) return;
    setOpLoading(true);
    try {
      if (isSuperAdmin) {
        await resetAdminPassword({ variables: { id: record.id, newPassword } });
      } else {
        await resetWorkerPassword({ variables: { id: record.id, newPassword } });
      }
      message.success('密码重置成功');
      setResetOpen(false);
    } catch {
      // 错误提示已由 hook onError 处理
    } finally {
      setOpLoading(false);
    }
  };

  const handleToggleStatus = () => {
    if (!record) return;
    const next: 'ACTIVE' | 'DISABLED' =
      record.accountStatus === 'DISABLED' ? 'ACTIVE' : 'DISABLED';
    const name = record.name || record.username || '';
    Modal.confirm({
      title: next === 'DISABLED' ? '确认停用' : '确认启用',
      content: next === 'DISABLED'
        ? `确定要停用账号 ${name} 吗？停用后该账号无法登录。`
        : `确定要启用账号 ${name} 吗？`,
      onOk: async () => {
        setOpLoading(true);
        try {
          if (isSuperAdmin) {
            await updateAdmin({
              variables: { id: record.id, input: { accountStatus: next } as UpdateAdminInput },
            });
          } else {
            await updateWorker({
              variables: { workerId: record.id, input: { accountStatus: next } as UpdateDeliveryWorkerInput },
            });
          }
          message.success(next === 'DISABLED' ? '账号已停用' : '账号已启用');
          await refetch();
        } catch {
          // 错误提示已由 hook onError 处理
        } finally {
          setOpLoading(false);
        }
      },
    });
  };

  const handleAdminSubmit = async (values: AdminFormData) => {
    if (!record) return;
    setOpLoading(true);
    try {
      await updateAdmin({
        variables: {
          id: record.id,
          input: {
            realName: values.realName,
            phone: values.phone,
            role: values.role,
            avatarUrl: values.avatarUrl,
            status: values.status,
            isAvailable: values.isAvailable,
            accountStatus: values.accountStatus ?? 'ACTIVE',
          },
        },
      });
      message.success('账号更新成功');
      setAdminFormVisible(false);
      await refetch();
    } catch {
      // 错误提示已由 hook onError 处理
    } finally {
      setOpLoading(false);
    }
  };

  const handleWorkerSubmit = async (values: DeliveryWorkerFormData) => {
    if (!record) return;
    setOpLoading(true);
    try {
      await updateWorker({
        variables: {
          workerId: record.id,
          input: {
            name: values.name,
            phone: values.phone,
            avatarUrl: values.avatarUrl,
            status: values.status,
            isAvailable: values.isAvailable,
            accountStatus: values.accountStatus ?? 'ACTIVE',
          },
        },
      });
      message.success('送水员信息更新成功');
      setWorkerFormVisible(false);
      await refetch();
    } catch {
      // 错误提示已由 hook onError 处理
    } finally {
      setOpLoading(false);
    }
  };

  const actions: AccountDetailActions = {
    onEdit: () => {
      if (isSuperAdmin) {
        setAdminFormVisible(true);
      } else {
        setWorkerFormVisible(true);
      }
    },
    onResetPassword: () => setResetOpen(true),
    onToggleStatus: handleToggleStatus,
    onDelete: handleDelete,
  };

  return {
    opLoading,
    resetOpen,
    adminFormVisible,
    workerFormVisible,
    actions,
    setResetOpen,
    setAdminFormVisible,
    setWorkerFormVisible,
    handleResetPassword,
    handleAdminSubmit,
    handleWorkerSubmit,
  };
};
