/**
 * 统一账号详情页记录形状(PRD/07-user.md §F-USR-020/021):
 * 送水员档案并入 admins 单表后,账号行即配送身份;
 * 超管视图来自 `admins` 查询(Admin 类型),普通管理员视图来自 `deliveryWorker(id:)`(DeliveryWorker 兼容投影)。
 */
export type AccountRole = 'SUPER_ADMIN' | 'ADMIN' | 'DELIVERY_WORKER';

export interface DeliveryRecord {
  id: string;
  /** 姓名(DeliveryWorker.name = Admin.realName) */
  name?: string;
  phone?: string;
  role: AccountRole;
  accountStatus: 'ACTIVE' | 'DISABLED';
  onlineStatus: 'ONLINE' | 'OFFLINE';
  isAvailable: boolean;
  avatarUrl?: string;
  coordinates?: string;
  currentLocation?: string;
  totalOrders: number;
  completedOrders: number;
  averageRating?: number | null;
  earningCents?: number | null;
  /** 当前任务数(待配送订单数;超管视图 Admin 类型暂不暴露,普通管理员视图来自 DeliveryWorker) */
  currentTaskCount?: number;
  // 管理账号专属(超管视图来自 Admin 类型)
  username?: string;
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 详情页操作区/账号安全卡回调(按登录角色 + 账号角色分发,见页面编排) */
export interface AccountDetailActions {
  onEdit: () => void;
  onResetPassword: () => void;
  onToggleStatus: () => void;
  onDelete: () => void;
}
