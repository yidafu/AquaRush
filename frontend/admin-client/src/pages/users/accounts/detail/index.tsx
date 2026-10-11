import React, { useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button, Spin } from '@/ui';
import { useQuery } from '@apollo/client';
import {
  GET_ADMIN_QUERY,
  DELIVERY_WORKER_DETAIL_QUERY,
} from '../../../../graphql/queries/user.graphql';
import type { AdminVo, DeliveryWorkerVo } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import AdminForm from '../../components/AdminForm';
import DeliveryWorkerForm from '../../components/DeliveryWorkerForm';
import ResetPasswordModal from '../../components/ResetPasswordModal';
import type {
  AdminFormData,
  DeliveryWorkerFormData,
} from '../../components/types';
import AdminAccountDetail from './AdminAccountDetail';
import WorkerAccountDetail from './WorkerAccountDetail';
import type { DeliveryRecord } from './types';
import { useAccountDetailActions } from './hooks/useAccountDetailActions';

const styles = stylex.create({
  statusWrap: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
    textAlign: 'center',
  },
});

/**
 * 统一账号详情页(users/accounts/:id,spec/prd/07-user.md §F-USR-020/021):
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
  } = useQuery(GET_ADMIN_QUERY, {
    variables: { id },
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
      const a = adminsData?.admin as AdminVo | undefined;
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
    const w: DeliveryWorkerVo | undefined = workerData?.deliveryWorker;
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
  }, [isSuperAdmin, adminsData, workerData]);

  const refetch = () => {
    if (isSuperAdmin) {
      refetchAdmins();
    } else {
      refetchWorker();
    }
  };

  const {
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
  } = useAccountDetailActions({ record, isSuperAdmin, refetch });

  // 普通管理员查看兼送水管理员(role=ADMIN)时,删除/重置密码不可用(后端仅允许 DELIVERY_WORKER,
  // 且普通管理员无 ADMIN_USER_* 权限 —— R-AUTH-04 UX 层隐藏,后端强制)
  const availableActions = useMemo(() => {
    if (isSuperAdmin) return undefined;
    if (record?.role === 'ADMIN') return { edit: true, toggleStatus: true };
    return undefined;
  }, [isSuperAdmin, record]);

  if (loading) {
    return (
      <div {...stylex.props(styles.statusWrap)}>
        <Spin size="large" />
      </div>
    );
  }

  if (!record) {
    return (
      <div {...stylex.props(styles.statusWrap)}>
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
          actions={actions}
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
