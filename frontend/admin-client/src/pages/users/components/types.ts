// User Management TypeScript Interfaces
// Based on GraphQL schema and backend models

export interface Admin {
  id: string;
  username: string;
  realName?: string;
  phone?: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'DELIVERY_WORKER';
  // 账号状态(停用后不可登录;F-USR-020)
  accountStatus: 'ACTIVE' | 'DISABLED';
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
  // 配送业务字段(送水员档案并入账号行,纯管理账号留空 —— F-USR-021 目标架构)
  avatarUrl?: string;
  onlineStatus?: 'ONLINE' | 'OFFLINE';
  isAvailable?: boolean;
}

export interface DeliveryWorker {
  id: string;
  userId: string;
  wechatOpenId: string;
  name: string;
  phone: string;
  avatarUrl?: string;
  status: 'ONLINE' | 'OFFLINE';
  // 账号状态(停用后不可派单/接单;F-USR-021)
  accountStatus: 'ACTIVE' | 'DISABLED';
  coordinates?: string;
  currentLocation?: string;
  rating?: number;
  totalOrders: number;
  completedOrders: number;
  averageRating?: number;
  earningCents?: number;
  isAvailable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  id: string;
  wechatOpenId: string;
  nickname?: string;
  phone?: string;
  avatarUrl?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DELETED';
  role: 'USER' | 'ADMIN' | 'WORKER' | 'NONE';
  customerGroupId?: string;
  customerGroupName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Address {
  id: string;
  receiverName: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

// Form interfaces for creation/editing
export interface AdminFormData {
  username?: string;
  realName?: string;
  phone?: string;
  password?: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'DELIVERY_WORKER';
  // 配送字段(送水员角色)
  avatarUrl?: string;
  status?: 'ONLINE' | 'OFFLINE';
  isAvailable?: boolean;
  // 账号状态(缺省 ACTIVE;F-USR-020)
  accountStatus?: 'ACTIVE' | 'DISABLED';
}

export interface DeliveryWorkerFormData {
  name: string;
  phone: string;
  wechatOpenId?: string;
  password?: string;
  avatarUrl?: string;
  status: 'ONLINE' | 'OFFLINE';
  isAvailable: boolean;
  accountStatus?: 'ACTIVE' | 'DISABLED';
}

export interface UserFormData {
  nickname?: string;
  phone?: string;
  wechatOpenId?: string;
  avatarUrl?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DELETED';
}

// Common interfaces
export interface TableActionProps {
  onEdit: (record: any) => void;
  onView?: (record: any) => void;
  onDelete: (record: any) => void;
  /** 可选:微信登录用户无密码,用户列表不提供重置密码(F-USR-022) */
  onResetPassword?: (record: any) => void;
  onSetGroup?: (record: any) => void;
}

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  loading?: boolean;
}

export interface FormModalProps<T> extends ModalProps {
  record?: T;
  onSubmit: (values: T) => Promise<void>;
  /** 当前登录者是否为超级管理员；非超管角色字段只读（后端以 PERMISSION_ADMIN_USER_WRITE 强制） */
  isSuperAdmin?: boolean;
}

// Role and status mappings for UI display
export const ROLE_MAPPINGS = {
  ADMIN: { color: 'red', text: '管理员' },
  CUSTOMER: { color: 'blue', text: '客户' },
  DELIVERY: { color: 'green', text: '配送员' },
  SUPER_ADMIN: { color: 'purple', text: '超级管理员' },
  DELIVERY_WORKER: { color: 'green', text: '送水员' },
  USER: { color: 'blue', text: '用户' },
  WORKER: { color: 'green', text: '配送员' },
  NONE: { color: 'default', text: '无' },
} as const;

export const STATUS_MAPPINGS = {
  ACTIVE: { color: 'success', text: '正常' },
  INACTIVE: { color: 'default', text: '未激活' },
  SUSPENDED: { color: 'warning', text: '已暂停' },
  DELETED: { color: 'error', text: '已删除' },
  BANNED: { color: 'error', text: '已禁用' },
  ONLINE: { color: 'processing', text: '在线' },
  OFFLINE: { color: 'default', text: '离线' },
} as const;

export const WORKER_STATUS_MAPPINGS = {
  ONLINE: { color: 'success', text: '在线' },
  OFFLINE: { color: 'default', text: '离线' },
} as const;