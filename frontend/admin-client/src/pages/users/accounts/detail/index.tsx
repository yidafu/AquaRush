import React, { useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button, Modal, Spin, message } from 'antd';
import { useQuery } from '@apollo/client';
import {
  GET_ADMINS_QUERY,
  DELIVERY_WORKER_DETAIL_QUERY,
} from '../../../../graphql/queries/user.graphql';
import type { Admin, DeliveryWorker } from '@aquarush/common';
import AdminForm from '../../components/AdminForm';
import DeliveryWorkerForm from '../../components/DeliveryWorkerForm';
import ResetPasswordModal from '../../components/ResetPasswordModal';
import type {
  AdminFormData,
  DeliveryWorkerFormData,
} from '../../components/types';
import {
  useDeleteAdmin,
  useResetAdminPassword,
  useUpdateAdmin,
  useDeleteDeliveryWorker,
  useResetDeliveryWorkerPassword,
  useUpdateDeliveryWorker,
  type UpdateAdminInput,
  type UpdateDeliveryWorkerInput,
} from '../../../../services/user-graphql';
import AdminAccountDetail from './AdminAccountDetail';
import WorkerAccountDetail from './WorkerAccountDetail';
import type { DeliveryRecord } from './types';

/**
 * 统一账号详情页(users/accounts/:id,PRD/07-user.md §F-USR-020/021):
 * 按角色渲染——DELIVERY_WORKER → 送水员视图;ADMIN/SUPER_ADMIN → 管理账号视图(含送水内容,F-USR-023)。
 * 数据源按登录角色切换:超管查 admins(全部账号)按 id 查找;普通管理员查 deliveryWorker(id:)(配送能力角色投影)。
 * 操作按权限矩阵分发:超管走 admin mutation,普通管理员走 worker mutation(R-AUTH-04 后端强制)。
 */
const AccountDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const isSuperAdmin = userInfo.role === 'SUPER_ADMIN';

  // 数据源
  const {
    data: adminsData,
    loading: adminsLoading,
    refetch: refetchAdmins,
  } = useQuery(GET_ADMINS_QUERY, {
    skip: !isSuperAdmin,
    errorPolicy: 'all',
  });
  const {
    data: workerData,
    loading: workerLoading,
    refetch: refetchWorker,
  } = useQuery(DELIVERY_WORKER_DETAIL_QUERY, {
    variables: { id },
    skip: isSuperAdmin,
    errorPolicy: 'all',
  });

  const loading = isSuperAdmin ? adminsLoading : workerLoading;

  const record: DeliveryRecord | undefined = useMemo(() => {
    if (isSuperAdmin) {
      const a = (adminsData?.admins || []).find((x: Admin) => String(x.id) === id);
      if (!a) return undefined;
      return {
        id: a.id,
        name: a.realName || undefined,
        phone: a.phone || undefined,
        role: a.role as DeliveryRecord['role'],
        accountStatus: a.accountStatus,
        onlineStatus: a.onlineStatus,
        isAvailable: a.isAvailable,
        avatarUrl: a.avatarUrl || undefined,
        coordinates: a.coordinates || undefined,
        currentLocation: a.currentLocation || undefined,
        totalOrders: a.totalOrders,
        completedOrders: a.completedOrders,
        averageRating: a.averageRating,
        earningCents: a.earningCents,
        username: a.username,
        lastLoginAt: a.lastLoginAt || undefined,
        createdAt: a.createdAt,
        updatedAt: a.updatedAt,
      };
    }
    const w: DeliveryWorker | undefined = workerData?.deliveryWorker;
    if (!w) return undefined;
    return {
      id: w.id,
      name: w.name,
      phone: w.phone,
      role: (w.role as DeliveryRecord['role']) || 'DELIVERY_WORKER',
      accountStatus: w.accountStatus,
      onlineStatus: w.onlineStatus,
      isAvailable: w.isAvailable,
      avatarUrl: w.avatarUrl || undefined,
      coordinates: w.coordinates || undefined,
      currentLocation: w.currentLocation || undefined,
      totalOrders: w.totalOrders,
      completedOrders: w.completedOrders,
      averageRating: w.averageRating,
      earningCents: w.earningCents,
      currentTaskCount: w.currentTaskCount,
      createdAt: w.createdAt,
      updatedAt: w.updatedAt,
    };
  }, [isSuperAdmin, adminsData, workerData, id]);

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

  const refetch = () => {
    if (isSuperAdmin) {
      refetchAdmins();
    } else {
      refetchWorker();
    }
  };

  // 普通管理员查看兼送水管理员(role=ADMIN)时,删除/重置密码不可用(后端仅允许 DELIVERY_WORKER,
  // 且普通管理员无 ADMIN_USER_* 权限 —— R-AUTH-04 UX 层隐藏,后端强制)
  const availableActions = useMemo(() => {
    if (isSuperAdmin) return undefined;
    if (record?.role === 'ADMIN') return { edit: true, toggleStatus: true };
    return undefined;
  }, [isSuperAdmin, record]);

  // ============ 操作处理 ============

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

  if (loading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!record) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <h2>加载失败</h2>
        <p>无法获取账号信息</p>
        <Button type="primary" onClick={() => navigate('/users/accounts')}>
          返回账号列表
        </Button>
      </div>
    );
  }

  return (
    <>
      {record.role === 'DELIVERY_WORKER' ? (
        <WorkerAccountDetail record={record} onBack={() => navigate('/users/accounts')} />
      ) : (
        <AdminAccountDetail
          record={record}
          onBack={() => navigate('/users/accounts')}
          actions={{
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
          }}
          availableActions={availableActions}
        />
      )}

      {/* 重置密码弹窗(新密码留空=默认 123456) */}
      <ResetPasswordModal
        targetName={record.name || record.username}
        open={resetOpen}
        confirmLoading={opLoading}
        onOk={handleResetPassword}
        onCancel={() => setResetOpen(false)}
      />

      {/* 超管编辑弹窗(统一账号表单) */}
      {isSuperAdmin && (
        <AdminForm
          open={adminFormVisible}
          onClose={() => setAdminFormVisible(false)}
          record={{
            id: record.id,
            username: record.username || '',
            realName: record.name,
            phone: record.phone,
            role: record.role,
            status: record.onlineStatus,
            accountStatus: record.accountStatus,
            lastLoginAt: record.lastLoginAt,
            createdAt: record.createdAt || '',
            updatedAt: record.updatedAt || '',
            avatarUrl: record.avatarUrl,
            onlineStatus: record.onlineStatus,
            isAvailable: record.isAvailable,
          } as AdminFormData}
          onSubmit={handleAdminSubmit}
          loading={opLoading}
          isSuperAdmin={isSuperAdmin}
        />
      )}

      {/* 普通管理员编辑弹窗(送水员表单,兼送水管理员同样可编辑配送字段) */}
      {!isSuperAdmin && (
        <DeliveryWorkerForm
          open={workerFormVisible}
          onClose={() => setWorkerFormVisible(false)}
          record={{
            id: record.id,
            userId: record.id,
            wechatOpenId: '',
            name: record.name || '',
            phone: record.phone || '',
            avatarUrl: record.avatarUrl,
            status: record.onlineStatus,
            onlineStatus: record.onlineStatus,
            accountStatus: record.accountStatus,
            coordinates: record.coordinates,
            currentLocation: record.currentLocation,
            isAvailable: record.isAvailable,
            totalOrders: record.totalOrders,
            completedOrders: record.completedOrders,
            createdAt: record.createdAt || '',
            updatedAt: record.updatedAt || '',
          } as DeliveryWorkerFormData}
          onSubmit={handleWorkerSubmit}
          loading={opLoading}
        />
      )}
    </>
  );
};

export default AccountDetailPage;
