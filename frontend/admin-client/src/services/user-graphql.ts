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
} from '../graphql/mutations/user.graphql';
import {
  CREATE_DELIVERY_WORKER_MUTATION,
  UPDATE_DELIVERY_WORKER_MUTATION,
} from '../graphql/mutations/delivery-worker.graphql';
import type {
  UserListInput,
} from '@aquarush/common';

export interface CreateAdminInput {
  readonly username: string;
  readonly password?: string;
  readonly realName?: string;
  readonly phone?: string;
  readonly role: string;
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
