import { useQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import {
  GET_USERS_QUERY,
  GET_ADMINS_QUERY,
  GET_DELIVERY_WORKERS_QUERY,
} from '../graphql/queries/user.graphql';
import {
  CREATE_ADMIN_MUTATION,
  UPDATE_ADMIN_MUTATION,
  DELETE_ADMIN_MUTATION,
  RESET_ADMIN_PASSWORD_MUTATION,
  CREATE_USER_MUTATION,
  UPDATE_USER_MUTATION,
  DELETE_USER_MUTATION,
} from '../graphql/mutations/user.graphql';
import {
  CREATE_DELIVERY_WORKER_MUTATION,
  UPDATE_DELIVERY_WORKER_MUTATION,
  DELETE_DELIVERY_WORKER_MUTATION,
  RESET_DELIVERY_WORKER_PASSWORD_MUTATION,
} from '../graphql/mutations/delivery-worker.graphql';
import type {
  UserListInput,
} from '@aquarush/common';

export interface CreateAdminInput {
  readonly username?: string;
  readonly password?: string;
  readonly realName?: string;
  readonly phone?: string;
  readonly role: string;
  // 配送字段(送水员角色,送水员档案并入账号行 —— F-USR-021)
  readonly avatarUrl?: string;
  readonly status?: 'ONLINE' | 'OFFLINE';
  readonly isAvailable?: boolean;
  // 账号状态(缺省 ACTIVE;F-USR-020)
  readonly accountStatus?: 'ACTIVE' | 'DISABLED';
}

export interface UpdateAdminInput {
  readonly realName?: string;
  readonly phone?: string;
  readonly role?: string;
  readonly avatarUrl?: string;
  readonly status?: 'ONLINE' | 'OFFLINE';
  readonly isAvailable?: boolean;
  readonly accountStatus?: 'ACTIVE' | 'DISABLED';
}

export interface CreateDeliveryWorkerInput {
  readonly name: string;
  readonly phone: string;
  readonly password?: string;
  readonly avatarUrl?: string;
  readonly wechatOpenId?: string;
  readonly coordinates?: string;
  readonly currentLocation?: string;
  readonly rating?: number;
  readonly earningCents?: number;
  readonly isAvailable?: boolean;
  readonly status?: 'ONLINE' | 'OFFLINE';
  readonly accountStatus?: 'ACTIVE' | 'DISABLED';
}

export interface UpdateDeliveryWorkerInput {
  readonly name?: string;
  readonly phone?: string;
  readonly avatarUrl?: string;
  readonly wechatOpenId?: string;
  readonly coordinates?: string;
  readonly currentLocation?: string;
  readonly rating?: number;
  readonly earningCents?: number;
  readonly isAvailable?: boolean;
  readonly status?: 'ONLINE' | 'OFFLINE';
  readonly accountStatus?: 'ACTIVE' | 'DISABLED';
}

export interface CreateUserInput {
  readonly nickname?: string;
  readonly phone?: string;
  readonly avatarUrl?: string;
}

export interface UpdateUserInput {
  readonly nickname?: string;
  readonly phone?: string;
  readonly avatarUrl?: string;
  readonly status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DELETED';
}

// User Query Hooks
export const useUsers = (input?: UserListInput) => {
  return useQuery(GET_USERS_QUERY, {
    variables: { input },
    errorPolicy: 'all',
    notifyOnNetworkStatusChange: true,
  });
};

export const useAdmins = () => {
  return useQuery(GET_ADMINS_QUERY, {
    errorPolicy: 'all',
  });
};

export const useDeliveryWorkers = () => {
  return useQuery(GET_DELIVERY_WORKERS_QUERY, {
    errorPolicy: 'all',
  });
};

// Admin Mutation Hooks
export const useCreateAdmin = () => {
  return useMutation(CREATE_ADMIN_MUTATION, {
    onCompleted: () => {
      message.success('管理员创建成功');
    },
    onError: (error) => {
      message.error(error.message || '管理员创建失败');
    },
    refetchQueries: ['GetAdmins'],
  });
};

export const useUpdateAdmin = () => {
  return useMutation(UPDATE_ADMIN_MUTATION, {
    onCompleted: () => {
      message.success('管理员信息更新成功');
    },
    onError: (error) => {
      message.error(error.message || '管理员信息更新失败');
    },
    refetchQueries: ['GetAdmins'],
  });
};

export const useDeleteAdmin = () => {
  return useMutation(DELETE_ADMIN_MUTATION, {
    onError: (error) => {
      message.error(error.message || '删除管理员失败');
    },
    refetchQueries: ['GetAdmins'],
  });
};

export const useResetAdminPassword = () => {
  return useMutation(RESET_ADMIN_PASSWORD_MUTATION, {
    onError: (error) => {
      message.error(error.message || '重置管理员密码失败');
    },
  });
};

// User Mutation Hooks (F-USR-022,G11 落地)
export const useCreateUser = () => {
  return useMutation(CREATE_USER_MUTATION, {
    onError: (error) => {
      message.error(error.message || '创建用户失败');
    },
    refetchQueries: ['GetUsers', 'CustomerUsersPageData'],
  });
};

export const useUpdateUser = () => {
  return useMutation(UPDATE_USER_MUTATION, {
    onError: (error) => {
      message.error(error.message || '更新用户失败');
    },
    refetchQueries: ['GetUsers', 'CustomerUsersPageData'],
  });
};

export const useDeleteUser = () => {
  return useMutation(DELETE_USER_MUTATION, {
    onError: (error) => {
      message.error(error.message || '删除用户失败');
    },
    refetchQueries: ['GetUsers', 'CustomerUsersPageData'],
  });
};

// Delivery Worker Mutation Hooks
export const useCreateDeliveryWorker = () => {
  return useMutation(CREATE_DELIVERY_WORKER_MUTATION, {
    onCompleted: () => {
      message.success('送水员创建成功');
    },
    onError: (error) => {
      message.error(error.message || '送水员创建失败');
    },
    refetchQueries: ['GetDeliveryWorkers'],
  });
};

export const useUpdateDeliveryWorker = () => {
  return useMutation(UPDATE_DELIVERY_WORKER_MUTATION, {
    onCompleted: () => {
      message.success('送水员信息更新成功');
    },
    onError: (error) => {
      message.error(error.message || '送水员信息更新失败');
    },
    refetchQueries: ['GetDeliveryWorkers'],
  });
};

export const useDeleteDeliveryWorker = () => {
  return useMutation(DELETE_DELIVERY_WORKER_MUTATION, {
    onError: (error) => {
      message.error(error.message || '删除送水员失败');
    },
    refetchQueries: ['GetDeliveryWorkers'],
  });
};

export const useResetDeliveryWorkerPassword = () => {
  return useMutation(RESET_DELIVERY_WORKER_PASSWORD_MUTATION, {
    onError: (error) => {
      message.error(error.message || '重置送水员密码失败');
    },
  });
};

