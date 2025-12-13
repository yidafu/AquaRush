// User Management TypeScript Interfaces
// Based on GraphQL schema and backend models

export interface Admin {
  id: string;
  username: string;
  realName?: string;
  phone?: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'NORMAL_ADMIN';
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DeliveryWorker {
  id: string;
  userId: string;
  wechatOpenId: string;
  name: string;
  phone: string;
  avatarUrl?: string;
  status: 'ONLINE' | 'OFFLINE';
  coordinates?: string;
  currentLocation?: string;
  rating?: number;
  totalOrders: number;
  completedOrders: number;
  averageRating?: number;
  earning?: number;
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
  username: string;
  realName?: string;
  phone?: string;
  password?: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'NORMAL_ADMIN';
}

export interface DeliveryWorkerFormData {
  name: string;
  phone: string;
  wechatOpenId: string;
  avatarUrl?: string;
  status: 'ONLINE' | 'OFFLINE';
  isAvailable: boolean;
}

export interface UserFormData {
  nickname?: string;
  phone?: string;
  wechatOpenId: string;
  avatarUrl?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DELETED';
}

// Common interfaces
export interface TableActionProps {
  onEdit: (record: any) => void;
  onDelete: (record: any) => void;
  onResetPassword: (record: any) => void;
}

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  loading?: boolean;
}

export interface FormModalProps<T> extends ModalProps {
  record?: T;
  onSubmit: (values: T) => Promise<void>;
}

// Role and status mappings for UI display
export const ROLE_MAPPINGS = {
  ADMIN: { color: 'red', text: '管理员' },
  CUSTOMER: { color: 'blue', text: '客户' },
  DELIVERY: { color: 'green', text: '配送员' },
  SUPER_ADMIN: { color: 'purple', text: '超级管理员' },
  NORMAL_ADMIN: { color: 'orange', text: '普通管理员' },
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