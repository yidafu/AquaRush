import { useQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import {
  GET_ORDERS_QUERY,
  GET_ORDER_DETAIL_QUERY,
  GET_ORDERS_BY_STATUS_QUERY,
} from '../graphql/queries/order.graphql';
import {
  UPDATE_ORDER_STATUS_MUTATION,
  ASSIGN_DELIVERY_WORKER_MUTATION,
  PROCESS_REFUND_MUTATION,
} from '../graphql/mutations/order.graphql';
import type {
  Order,
  OrderListInput,
  OrderStatus,
  ProcessRefundInput,
} from '../types/graphql';

// Order Query Hooks
export const useOrders = (input?: OrderListInput) => {
  return useQuery(GET_ORDERS_QUERY, {
    variables: { input },
    errorPolicy: 'all',
    notifyOnNetworkStatusChange: true,
  });
};

export const useOrderDetail = (id: number) => {
  return useQuery(GET_ORDER_DETAIL_QUERY, {
    variables: { id },
    skip: !id,
    errorPolicy: 'all',
  });
};

export const useOrdersByStatus = (status: OrderStatus) => {
  return useQuery(GET_ORDERS_BY_STATUS_QUERY, {
    variables: { status },
    errorPolicy: 'all',
  });
};

// Order Mutation Hooks
export const useUpdateOrderStatus = () => {
  return useMutation(UPDATE_ORDER_STATUS_MUTATION, {
    onCompleted: () => {
      message.success('订单状态更新成功');
    },
    onError: (error) => {
      message.error(error.message || '订单状态更新失败');
    },
    refetchQueries: ['GetOrders', 'GetOrdersByStatus'],
  });
};

export const useAssignDeliveryWorker = () => {
  return useMutation(ASSIGN_DELIVERY_WORKER_MUTATION, {
    onCompleted: () => {
      message.success('配送员分配成功');
    },
    onError: (error) => {
      message.error(error.message || '配送员分配失败');
    },
    refetchQueries: ['GetOrders', 'GetOrderDetail'],
  });
};

export const useProcessRefund = () => {
  return useMutation(PROCESS_REFUND_MUTATION, {
    onCompleted: () => {
      message.success('退款处理成功');
    },
    onError: (error) => {
      message.error(error.message || '退款处理失败');
    },
    refetchQueries: ['GetOrders', 'GetOrderDetail'],
  });
};