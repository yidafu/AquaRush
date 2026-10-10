import React, { useMemo, useState } from 'react';
import { Modal, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@apollo/client';
import AdminForm from '../components/AdminForm';
import DeliveryWorkerForm from '../components/DeliveryWorkerForm';
import ResetPasswordModal from '../components/ResetPasswordModal';
import {
  GET_ADMINS_QUERY,
  GET_DELIVERY_WORKERS_QUERY,
} from '../../../graphql/queries/user.graphql';
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
} from '../../../services/user-graphql';
import type { AdminVo as AdminDTO, DeliveryWorkerVo as DeliveryWorkerDTO } from '@aquarush/common';
import type {
  AdminFormData,
  DeliveryWorkerFormData,
} from '../components/types';
import { AccountToolbar, AccountTable } from './components';
import type { AccountRow, EditingAdmin, EditingWorker } from './types';

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
  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(false);
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

  // Mutations
  const [createAdmin, { loading: createAdminLoading }] = useCreateAdmin();
  const [updateAdmin, { loading: updateAdminLoading }] = useUpdateAdmin();
  const [deleteAdmin] = useDeleteAdmin();
  const [resetAdminPassword] = useResetAdminPassword();
  const [createWorker, { loading: createWorkerLoading }] = useCreateDeliveryWorker();
  const [updateWorker, { loading: updateWorkerLoading }] = useUpdateDeliveryWorker();
  const [deleteWorker] = useDeleteDeliveryWorker();
  const [resetWorkerPassword] = useResetDeliveryWorkerPassword();

  // 表单/弹窗状态
  const [adminFormVisible, setAdminFormVisible] = useState<boolean>(false);
  const [workerFormVisible, setWorkerFormVisible] = useState<boolean>(false);
  const [resetTarget, setResetTarget] = useState<AccountRow | null>(null);
  const [editingAdmin, setEditingAdmin] = useState<EditingAdmin | null>(null);
  const [editingWorker, setEditingWorker] = useState<EditingWorker | null>(null);

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
  const roleFilterOptions = useMemo(() => {
    if (isSuperAdmin) {
      return [
        { value: 'ALL', label: '全部' },
        { value: 'SUPER_ADMIN', label: '超级管理员' },
        { value: 'ADMIN', label: '管理员' },
        { value: 'DELIVERY_WORKER', label: '送水员' },
      ];
    }
    return [
      { value: 'ALL', label: '全部' },
      { value: 'ADMIN', label: '兼送水管理员' },
      { value: 'DELIVERY_WORKER', label: '送水员' },
    ];
  }, [isSuperAdmin]);

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

  // 普通管理员视图:兼送水管理员(role=ADMIN)不可删除/重置密码(后端仅允许 DELIVERY_WORKER 账号行)
  const canDeleteOrReset = (record: AccountRow) => isSuperAdmin || record.role === 'DELIVERY_WORKER';

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

  const handleResetPassword = async (newPassword?: string) => {
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
  const handleAdminSubmit = async (values: AdminFormData) => {
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
  const handleWorkerSubmit = async (values: DeliveryWorkerFormData) => {
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
