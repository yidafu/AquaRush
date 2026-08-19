import type {
  AdminFormData,
  DeliveryWorkerFormData,
} from '../components/types';

/**
 * 账号管理页(users/accounts)类型定义:
 * 表格行结构与编辑态记录由列表页与表格组件共享。
 */
export type AccountRole = 'SUPER_ADMIN' | 'ADMIN' | 'DELIVERY_WORKER';

export interface AccountRow {
  id: string;
  username?: string;
  realName?: string;
  name?: string;
  phone?: string;
  role: AccountRole;
  avatarUrl?: string;
  onlineStatus?: 'ONLINE' | 'OFFLINE';
  isAvailable?: boolean;
  accountStatus?: 'ACTIVE' | 'DISABLED';
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

/** 编辑态记录:表单数据 + 主键/时间戳(供 handleAdminSubmit 判断新建/更新) */
export type EditingAdmin = AdminFormData & {
  id: string;
  createdAt: string;
  updatedAt: string;
  lastLoginAt?: string;
};

export type EditingWorker = DeliveryWorkerFormData & {
  id: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
};
