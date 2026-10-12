/**
 * 账号管理列表页操作逻辑(users/accounts/index.tsx 拆分):
 * 增删改/重置密码 mutation 与视图/编辑/删除处理。
 */
import { useState } from 'react';
import { Modal, message } from '@/ui';
import { useNavigate } from 'react-router-dom';
import {
  useCreateAdmin,
  useUpdateAdmin,
  useDeleteAdmin,
  useResetAdminPassword,
  useCreateDeliveryWorker,
  useUpdateDeliveryWorker,
  useDeleteDeliveryWorker,
  useResetDeliveryWorkerPassword,
  type CreateAdminInput,
  type CreateDeliveryWorkerInput,
  type UpdateAdminInput,
  type UpdateDeliveryWorkerInput,
} from '../../../../services/user-graphql';
import type { AdminFormData, DeliveryWorkerFormData } from '../../components/types';
import type { AccountRow, EditingAdmin, EditingWorker } from '../types';

export interface UseAccountActionsOptions {
  isSuperAdmin: boolean;
  refetchAdmins: () => Promise<unknown>;
  refetchWorkers: () => Promise<unknown>;
  editingAdmin: EditingAdmin | null;
  editingWorker: EditingWorker | null;
  resetTarget: AccountRow | null;
  setAdminFormVisible: (visible: boolean) => void;
  setEditingAdmin: (record: EditingAdmin | null) => void;
  setWorkerFormVisible: (visible: boolean) => void;
  setEditingWorker: (record: EditingWorker | null) => void;
  setResetTarget: (record: AccountRow | null) => void;
}

export interface UseAccountActionsResult {
  loading: boolean;
  createAdminLoading: boolean;
  updateAdminLoading: boolean;
  createWorkerLoading: boolean;
  updateWorkerLoading: boolean;
  handleView: (record: AccountRow) => void;
  handleEdit: (record: AccountRow) => void;
  handleDelete: (record: AccountRow) => void;
  handleResetPassword: (newPassword?: string) => Promise<void>;
  handleAdminSubmit: (values: AdminFormData) => Promise<void>;
  handleWorkerSubmit: (values: DeliveryWorkerFormData) => Promise<void>;
}

export function useAccountActions(
  options: UseAccountActionsOptions,
): UseAccountActionsResult {
  const {
    isSuperAdmin,
    refetchAdmins,
    refetchWorkers,
    editingAdmin,
    editingWorker,
    resetTarget,
    setAdminFormVisible,
    setEditingAdmin,
    setWorkerFormVisible,
    setEditingWorker,
    setResetTarget,
  } = options;
  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(false);

  // Mutations
  const [createAdmin, { loading: createAdminLoading }] = useCreateAdmin();
  const [updateAdmin, { loading: updateAdminLoading }] = useUpdateAdmin();
  const [deleteAdmin] = useDeleteAdmin();
  const [resetAdminPassword] = useResetAdminPassword();
  const [createWorker, { loading: createWorkerLoading }] = useCreateDeliveryWorker();
  const [updateWorker, { loading: updateWorkerLoading }] = useUpdateDeliveryWorker();
  const [deleteWorker] = useDeleteDeliveryWorker();
  const [resetWorkerPassword] = useResetDeliveryWorkerPassword();

  const refetch = () => {
    if (isSuperAdmin) {
      refetchAdmins();
    } else {
      refetchWorkers();
    }
  };

  // 操作处理

  const handleView = (record: AccountRow) => {
    // 统一跳转独立详情页 users/accounts/:id(按角色渲染,2026-08 设计调整)
    navigate(`/users/accounts/${record.id}`);
  };

  const handleEdit = (record: AccountRow) => {
    if (isSuperAdmin) {
      setEditingAdmin({
        id: record.id,
        username: record.username || '',
        realName: record.realName,
        phone: record.phone,
        role: record.role,
        status: record.onlineStatus,
        accountStatus: record.accountStatus ?? 'ACTIVE',
        lastLoginAt: record.lastLoginAt,
        createdAt: record.createdAt,
        updatedAt: record.updatedAt,
        avatarUrl: record.avatarUrl,
        isAvailable: record.isAvailable,
      });
      setAdminFormVisible(true);
    } else {
      setEditingWorker({
        id: record.id,
        userId: record.id,
        wechatOpenId: '',
        name: record.name || record.realName || record.username || '',
        phone: record.phone || '',
        avatarUrl: record.avatarUrl,
        status: record.onlineStatus || 'OFFLINE',
        accountStatus: record.accountStatus ?? 'ACTIVE',
        isAvailable: record.isAvailable ?? true,
        createdAt: record.createdAt,
        updatedAt: record.updatedAt,
      });
      setWorkerFormVisible(true);
    }
  };

  const handleDelete = (record: AccountRow) => {
    const name = record.realName || record.name || record.username || '';
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除${record.role === 'DELIVERY_WORKER' ? '送水员' : '账号'} ${name} 吗？此操作不可恢复。`,
      okType: 'danger',
      onOk: async () => {
        setLoading(true);
        try {
          if (isSuperAdmin) {
            await deleteAdmin({ variables: { id: record.id } });
          } else {
            await deleteWorker({ variables: { id: record.id } });
          }
          message.success('删除成功');
          await refetch();
        } catch {
          // 错误提示已由 hook onError 处理
        } finally {
          setLoading(false);
        }
      },
    });
  };

  const handleResetPassword = async (newPassword?: string): Promise<void> => {
    if (!resetTarget) return;
    setLoading(true);
    try {
      if (isSuperAdmin) {
        await resetAdminPassword({ variables: { id: resetTarget.id, newPassword } });
      } else {
        await resetWorkerPassword({ variables: { id: resetTarget.id, newPassword } });
      }
      message.success('密码重置成功');
      setResetTarget(null);
    } catch {
      // 错误提示已由 hook onError 处理
    } finally {
      setLoading(false);
    }
  };

  // 超管表单提交(create/updateAdmin,送水员角色含配送字段)
  const handleAdminSubmit = async (values: AdminFormData): Promise<void> => {
    setLoading(true);
    try {
      // 非超管强制沿用现有角色(后端以 PERMISSION_ADMIN_USER_WRITE 拦截为准)
      const role = isSuperAdmin ? values.role : (editingAdmin?.role ?? values.role);
      // 送水员角色用户名留空由后端自动生成 delivery_{账号ID}(F-USR-021 单表架构)
      const input: CreateAdminInput = {
        username: values.username || undefined,
        realName: values.realName,
        phone: values.phone,
        password: values.password,
        role,
        avatarUrl: values.avatarUrl,
        status: values.status,
        isAvailable: values.isAvailable,
        accountStatus: values.accountStatus ?? 'ACTIVE',
      };

      if (editingAdmin && editingAdmin.id) {
        const updateInput: UpdateAdminInput = {
          realName: input.realName,
          phone: input.phone,
          role: input.role,
          avatarUrl: input.avatarUrl,
          status: input.status,
          isAvailable: input.isAvailable,
          accountStatus: input.accountStatus,
        };
        await updateAdmin({ variables: { id: editingAdmin.id, input: updateInput } });
        message.success('账号更新成功');
      } else {
        await createAdmin({ variables: { input } });
        message.success('账号创建成功');
      }
      await refetchAdmins();
      setAdminFormVisible(false);
      setEditingAdmin(null);
    } catch (error) {
      console.error('Account submit error:', error);
      message.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  // 普通管理员表单提交(create/updateDeliveryWorker)
  const handleWorkerSubmit = async (values: DeliveryWorkerFormData): Promise<void> => {
    setLoading(true);
    try {
      const input: CreateDeliveryWorkerInput = {
        name: values.name,
        phone: values.phone,
        password: values.password,
        avatarUrl: values.avatarUrl,
        wechatOpenId: values.wechatOpenId || undefined,
        isAvailable: values.isAvailable,
        status: values.status,
        accountStatus: values.accountStatus ?? 'ACTIVE',
      };

      if (editingWorker && editingWorker.id) {
        const updateInput: UpdateDeliveryWorkerInput = {
          name: input.name,
          phone: input.phone,
          avatarUrl: input.avatarUrl,
          isAvailable: input.isAvailable,
          status: input.status,
          accountStatus: input.accountStatus,
        };
        await updateWorker({ variables: { workerId: editingWorker.id, input: updateInput } });
        message.success('送水员信息更新成功');
      } else {
        await createWorker({ variables: { input } });
        message.success('送水员创建成功');
      }
      await refetchWorkers();
      setWorkerFormVisible(false);
      setEditingWorker(null);
    } catch (error) {
      console.error('Delivery worker submit error:', error);
      message.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,
    createAdminLoading,
    updateAdminLoading,
    createWorkerLoading,
    updateWorkerLoading,
    handleView,
    handleEdit,
    handleDelete,
    handleResetPassword,
    handleAdminSubmit,
    handleWorkerSubmit,
  };
}
