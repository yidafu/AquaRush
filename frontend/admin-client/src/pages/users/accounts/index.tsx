import React, { useMemo, useState } from 'react';
import { useQuery } from '@apollo/client';
import AdminForm from '../components/AdminForm';
import DeliveryWorkerForm from '../components/DeliveryWorkerForm';
import ResetPasswordModal from '../components/ResetPasswordModal';
import {
  GET_ADMINS_QUERY,
  GET_DELIVERY_WORKERS_QUERY,
} from '../../../graphql/queries/user.graphql';
import type { AdminVo as AdminDTO, DeliveryWorkerVo as DeliveryWorkerDTO } from '@aquarush/common';
import { AccountToolbar, AccountTable } from './components';
import type { AccountRow, EditingAdmin, EditingWorker } from './types';
import { roleFilterOptionsFor } from './helpers';
import { useAccountActions } from './hooks/useAccountActions';

/**
 * 账号管理(管理员管理与送水员管理合并,spec/prd/07-user.md §F-USR-020/021):
 * - 数据源按登录角色切换:超管查 admins(账号分页列表,含送水员);普通管理员查 deliveryWorkers(配送能力角色),
 *   权限隔离以 PERMISSION_ADMIN_USER_* / PERMISSION_DELIVERY_WORKER_* 后端强制(R-AUTH-04);
 * - 统一表单:超管走 create/updateAdmin(送水员角色展示配送字段);普通管理员走 create/updateDeliveryWorker;
 * - 2026-08 设计调整:角色 Tab →「用户类型」过滤参数(下拉 Select,无 Tab);「查看」统一跳转
 *   独立详情页 users/accounts/:id(按角色渲染:管理账号→管理员详情、送水员→送水员详情,见 F-USR-020/021);
 * - 删除/重置密码走真实接口(超管 deleteAdmin/resetAdminPassword;普通管理员 deleteDeliveryWorker/
 *   resetDeliveryWorkerPassword,后端仅允许 DELIVERY_WORKER 账号行)。
 */
const AccountUsers: React.FC = () => {
  const [activeRole, setActiveRole] = useState<string>('ALL');
  // 服务端分页/过滤状态(AGENTS.md 规则 10:列表接口分页 + 过滤)
  const [keyword, setKeyword] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(20);

  // 仅超级管理员可管理管理员账号(菜单对普通管理员隐藏管理员项,后端以权限强制)
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  const isSuperAdmin = userInfo.role === 'SUPER_ADMIN';

  // 数据源按角色切换:超管查 admins 分页账号;普通管理员查 deliveryWorkers(配送能力角色);
  // 分页 + keyword/role 过滤全部下推服务端(AGENTS.md 规则 10)
  const queryVariables = {
    keyword: keyword || null,
    role: activeRole === 'ALL' ? null : activeRole,
    page,
    size,
  };
  const { data: adminsData, loading: adminsLoading, refetch: refetchAdmins } = useQuery(
    GET_ADMINS_QUERY,
    { skip: !isSuperAdmin, errorPolicy: 'all', variables: queryVariables },
  );
  const { data: workersData, loading: workersLoading, refetch: refetchWorkers } = useQuery(
    GET_DELIVERY_WORKERS_QUERY,
    { skip: isSuperAdmin, errorPolicy: 'all', variables: queryVariables },
  );

  const adminsPage = adminsData?.admins;
  const workersPage = workersData?.deliveryWorkers;
  const totalElements = (isSuperAdmin ? adminsPage?.totalElements : workersPage?.totalElements) ?? 0;

  // 表单/弹窗状态
  const [adminFormVisible, setAdminFormVisible] = useState<boolean>(false);
  const [workerFormVisible, setWorkerFormVisible] = useState<boolean>(false);
  const [resetTarget, setResetTarget] = useState<AccountRow | null>(null);
  const [editingAdmin, setEditingAdmin] = useState<EditingAdmin | null>(null);
  const [editingWorker, setEditingWorker] = useState<EditingWorker | null>(null);

  // 操作逻辑(mutation + 视图/编辑/删除/提交处理,由 index.tsx 拆分)
  const {
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
  } = useAccountActions({
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
  });

  const accounts: AccountRow[] = useMemo(() => {
    if (isSuperAdmin) {
      return (adminsPage?.content || []).map((a: AdminDTO) => ({
        id: a.id,
        username: a.username,
        realName: a.realName,
        phone: a.phone,
        role: a.role as AccountRow['role'],
        avatarUrl: a.avatarUrl || undefined,
        onlineStatus: a.onlineStatus,
        isAvailable: a.isAvailable,
        accountStatus: a.accountStatus,
        lastLoginAt: a.lastLoginAt || undefined,
        createdAt: a.createdAt,
        updatedAt: a.updatedAt,
      }));
    }
    return (workersPage?.content || []).map((w: DeliveryWorkerDTO) => ({
      id: w.id,
      name: w.name,
      phone: w.phone,
      role: (w.role as AccountRow['role']) || 'DELIVERY_WORKER',
      avatarUrl: w.avatarUrl || undefined,
      onlineStatus: w.onlineStatus,
      isAvailable: w.isAvailable,
      accountStatus: w.accountStatus,
      createdAt: w.createdAt,
      updatedAt: w.updatedAt,
    }));
  }, [isSuperAdmin, adminsPage, workersPage]);

  const loadingAccounts = isSuperAdmin ? adminsLoading : workersLoading;

  // 「用户类型」过滤参数(2026-08 设计调整:替代原角色 Tab,无 Tab 切换)
  const roleFilterOptions = useMemo(() => roleFilterOptionsFor(isSuperAdmin), [isSuperAdmin]);

  // 角色过滤已下推服务端(role 参数),这里直接用当前页数据
  const filtered = accounts;

  const handleRoleChange = (value: string) => {
    setActiveRole(value);
    setPage(0);
  };

  const handleKeywordChange = (value: string) => {
    setKeyword(value.trim());
    setPage(0);
  };

  const handlePageChange = (nextPage: number, nextSize: number) => {
    setPage(nextPage);
    setSize(nextSize);
  };

  // 普通管理员视图:兼送水管理员(role=ADMIN)不可删除/重置密码(后端仅允许 DELIVERY_WORKER 账号行)
  const canDeleteOrReset = (record: AccountRow) => isSuperAdmin || record.role === 'DELIVERY_WORKER';

  return (
    <div>
      <AccountToolbar
        activeRole={activeRole}
        roleFilterOptions={roleFilterOptions}
        isSuperAdmin={isSuperAdmin}
        onRoleChange={handleRoleChange}
        keyword={keyword}
        onKeywordChange={handleKeywordChange}
        onAdd={() => {
          if (isSuperAdmin) {
            setEditingAdmin(null);
            setAdminFormVisible(true);
          } else {
            setEditingWorker(null);
            setWorkerFormVisible(true);
          }
        }}
      />

      <AccountTable
        accounts={filtered}
        loading={loadingAccounts}
        canDeleteOrReset={canDeleteOrReset}
        onView={handleView}
        onEdit={handleEdit}
        onDelete={handleDelete}
        onReset={setResetTarget}
        pagination={{ total: totalElements, page, size, onChange: handlePageChange }}
      />

      {/* 重置密码弹窗(新密码留空=默认 123456,G11 落地) */}
      <ResetPasswordModal
        targetName={resetTarget?.realName || resetTarget?.name || resetTarget?.username}
        open={resetTarget !== null}
        confirmLoading={loading}
        onOk={handleResetPassword}
        onCancel={() => setResetTarget(null)}
      />

      {/* 超管表单:统一账号表单(送水员角色含配送字段) */}
      {isSuperAdmin && (
        <AdminForm
          open={adminFormVisible}
          onClose={() => {
            setAdminFormVisible(false);
            setEditingAdmin(null);
          }}
          record={editingAdmin || undefined}
          onSubmit={handleAdminSubmit}
          loading={loading || createAdminLoading || updateAdminLoading}
          isSuperAdmin={isSuperAdmin}
        />
      )}

      {/* 普通管理员表单:送水员表单 */}
      {!isSuperAdmin && (
        <DeliveryWorkerForm
          open={workerFormVisible}
          onClose={() => {
            setWorkerFormVisible(false);
            setEditingWorker(null);
          }}
          record={editingWorker || undefined}
          onSubmit={handleWorkerSubmit}
          loading={loading || createWorkerLoading || updateWorkerLoading}
        />
      )}
    </div>
  );
};

export default AccountUsers;
