import { useQuery, useMutation, useLazyQuery } from '@apollo/client';
import { message } from 'antd';
import {
  GET_USERS_QUERY,
  GET_USER_DETAIL_QUERY,
  GET_ADMINS_QUERY,
  GET_DELIVERY_WORKERS_QUERY,
  DELIVERY_WORKER_DETAIL_QUERY,
  DELIVERY_WORKER_STATISTICS_QUERY,
  DELIVERY_WORKER_ORDERS_QUERY,
  USER_ORDERS_QUERY,
} from '../graphql/queries/user.graphql';
import {
  UPDATE_USER_MUTATION,
  CREATE_USER_MUTATION,
  DELETE_USER_MUTATION,
  TOGGLE_USER_STATUS_MUTATION,
  CREATE_ADMIN_MUTATION,
  UPDATE_ADMIN_MUTATION,
} from '../graphql/mutations/user.graphql';
import type {
  User,
  UserListInput,
  UpdateUserInput,
  CreateUserInput,
  UserStatus,
  Admin,
  DeliveryWorker,
  CreateAdminInput,
  UpdateAdminInput,
} from '../types/graphql';

// User Query Hooks
export const useUsers = (input?: UserListInput) => {
  return useQuery(GET_USERS_QUERY, {
    variables: { input },
    errorPolicy: 'all',
    notifyOnNetworkStatusChange: true,
  });
};

export const useUserDetail = (id: number) => {
  return useQuery(GET_USER_DETAIL_QUERY, {
    variables: { id },
    skip: !id,
    errorPolicy: 'all',
  });
};

export const useLazyUsers = () => {
  return useLazyQuery(GET_USERS_QUERY, {
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

// User Mutation Hooks
export const useUpdateUser = () => {
  return useMutation(UPDATE_USER_MUTATION, {
    onCompleted: () => {
      message.success('用户信息更新成功');
    },
    onError: (error) => {
      message.error(error.message || '用户信息更新失败');
    },
    refetchQueries: ['GetUsers'], // 自动刷新用户列表
  });
};

export const useCreateUser = () => {
  return useMutation(CREATE_USER_MUTATION, {
    onCompleted: () => {
      message.success('用户创建成功');
    },
    onError: (error) => {
      message.error(error.message || '用户创建失败');
    },
    refetchQueries: ['GetUsers'],
  });
};

export const useDeleteUser = () => {
  return useMutation(DELETE_USER_MUTATION, {
    onCompleted: () => {
      message.success('用户删除成功');
    },
    onError: (error) => {
      message.error(error.message || '用户删除失败');
    },
    refetchQueries: ['GetUsers'],
  });
};

export const useToggleUserStatus = () => {
  return useMutation(TOGGLE_USER_STATUS_MUTATION, {
    onCompleted: () => {
      message.success('用户状态更新成功');
    },
    onError: (error) => {
      message.error(error.message || '用户状态更新失败');
    },
    refetchQueries: ['GetUsers'],
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

// Delivery Worker Detail Query Hooks
export const useDeliveryWorkerDetail = (id: number) => {
  return useQuery(DELIVERY_WORKER_DETAIL_QUERY, {
    variables: { id },
    skip: !id,
    errorPolicy: 'all',
  });
};

export const useDeliveryWorkerStatistics = (deliveryWorkerId: number) => {
  return useQuery(DELIVERY_WORKER_STATISTICS_QUERY, {
    variables: { deliveryWorkerId },
    skip: !deliveryWorkerId,
    errorPolicy: 'all',
  });
};

export const useDeliveryWorkerOrders = (deliveryWorkerId: number, status?: string) => {
  return useQuery(DELIVERY_WORKER_ORDERS_QUERY, {
    variables: { deliveryWorkerId, status },
    skip: !deliveryWorkerId,
    errorPolicy: 'all',
  });
};

export const useUserOrders = (userId: number) => {
  return useQuery(USER_ORDERS_QUERY, {
    variables: { userId },
    skip: !userId,
    errorPolicy: 'all',
  });
};

// Utility functions for manual cache updates
export const updateUserCache = (userId: number, updatedData: Partial<User>) => {
  // This would be used for optimistic updates
  // Implementation depends on your cache structure
};