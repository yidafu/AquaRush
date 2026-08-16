import React, { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { useQuery, useMutation } from '@apollo/client';
import { message } from 'antd';
import { ORDER_DETAIL_PAGE_DATA_QUERY } from '../../../graphql/queries/order.graphql';
import { ASSIGN_DELIVERY_WORKER_MUTATION } from '../../../graphql/mutations/order.graphql';
import type { OrderOperation } from '../../../components/OrderOperationTimeline';

export interface OrderDetailData {
  id: number;
  orderNo: string;
  user: {
    id: number;
    nickname: string;
    phone: string;
    avatarUrl?: string;
  };
  status: string;
  amount: number;
  items: Array<{
    productId: number;
    quantity: number;
    unitPriceCents?: number;
    totalPriceCents?: number;
    product?: {
      id: number;
      name: string;
      coverImageUrl?: string;
      specification?: string;
      priceCents?: number;
    };
  }>;
  address: {
    receiverName: string;
    phone: string;
    province: string;
    city: string;
    district: string;
    detailAddress: string;
    longitude?: number;
    latitude?: number;
  };
  deliveryWorker?: {
    id: number;
    name: string;
    phone: string;
    avatarUrl?: string;
  };
  paymentMethod?: string;
  paymentTransactionId?: string;
  paymentTime?: string;
  paymentType?: string;
  deliveryPhotos?: string[];
  isSelfCollect: boolean;
  deliveryStartedAt?: string;
  deliveryConfirmedAt?: string;
  completedAt?: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface OrderDetailQueryResult {
  order: OrderDetailData;
  onlineDeliveryWorkers: DeliveryWorker[];
  orderOperations: OrderOperation[];
}

export interface DeliveryWorker {
  id: number;
  name: string;
  phone: string;
  avatarUrl?: string;
}

export const ORDER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: '待支付', color: 'default' },
  PENDING_DELIVERY: { text: '待配送', color: 'blue' },
  DELIVERING: { text: '配送中', color: 'processing' },
  COMPLETED: { text: '已完成', color: 'green' },
  CANCELLED: { text: '已取消', color: 'red' },
  REFUNDED: { text: '已退款', color: 'magenta' },
};

interface OrderDetailContextType {
  // Order data
  order: OrderDetailData | null;
  orderOperations: OrderOperation[];
  workers: DeliveryWorker[];

  // Loading states
  loading: boolean;
  workersLoading: boolean;
  assignLoading: boolean;
  error: Error | null;

  // Modal state
  assignModalVisible: boolean;
  selectedWorkerId: number | null;
  isSelfCollect: boolean;

  // Actions
  loadOrder: (id: number) => void;
  refreshOrder: () => void;
  openAssignModal: () => void;
  closeAssignModal: () => void;
  setSelectedWorkerId: (id: number | null) => void;
  setIsSelfCollect: (value: boolean) => void;
  assignDeliveryWorker: () => Promise<void>;
}

const OrderDetailContext = createContext<OrderDetailContextType | undefined>(undefined);

interface OrderDetailProviderProps {
  children: ReactNode;
  orderId: string;
}

export const OrderDetailProvider: React.FC<OrderDetailProviderProps> = ({ children, orderId }) => {
  // Modal state
  const [assignModalVisible, setAssignModalVisible] = useState(false);
  const [selectedWorkerId, setSelectedWorkerId] = useState<number | null>(null);
  const [isSelfCollect, setIsSelfCollect] = useState(false);

  // 单页单请求:订单详情 + 在线配送员 + 操作记录一次取回(规则 #10);
  // orderId 变化经 variables 变化自动重取,refreshOrder = refetch
  const { data, loading, error, refetch } = useQuery<OrderDetailQueryResult>(ORDER_DETAIL_PAGE_DATA_QUERY, {
    variables: { orderId: parseInt(orderId, 10) },
    skip: !orderId,
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });

  // Mutation for assigning delivery worker
  const [assignDeliveryWorkerMutation, { loading: assignLoading }] = useMutation(ASSIGN_DELIVERY_WORKER_MUTATION);

  const order = data?.order ?? null;
  const workers: DeliveryWorker[] = data?.onlineDeliveryWorkers ?? [];
  const orderOperations: OrderOperation[] = data?.orderOperations ?? [];

  const loadOrder = useCallback((id: number) => {
    refetch({ orderId: id });
  }, [refetch]);

  const refreshOrder = useCallback(() => {
    if (orderId) {
      refetch({ orderId: parseInt(orderId, 10) });
    }
  }, [orderId, refetch]);

  const openAssignModal = useCallback(() => {
    setSelectedWorkerId(null);
    setIsSelfCollect(order?.isSelfCollect ?? false);
    setAssignModalVisible(true);
  }, [order?.isSelfCollect]);

  const closeAssignModal = useCallback(() => {
    setAssignModalVisible(false);
  }, []);

  const assignDeliveryWorkerAction = useCallback(async () => {
    if (!selectedWorkerId) {
      message.warning('请选择配送员');
      return;
    }

    try {
      await assignDeliveryWorkerMutation({
        variables: {
          orderId: parseInt(orderId, 10),
          workerId: selectedWorkerId,
          isSelfCollect,
        },
      });
      message.success('分配配送员成功');
      setAssignModalVisible(false);
      // Refresh order data
      refreshOrder();
    } catch (err: any) {
      message.error(err.message || '分配配送员失败');
    }
  }, [selectedWorkerId, isSelfCollect, orderId, assignDeliveryWorkerMutation, refreshOrder]);

  const value: OrderDetailContextType = {
    order,
    orderOperations,
    workers,
    loading,
    workersLoading: loading,
    assignLoading,
    error: error ?? null,
    assignModalVisible,
    selectedWorkerId,
    isSelfCollect,
    loadOrder,
    refreshOrder,
    openAssignModal,
    closeAssignModal,
    setSelectedWorkerId,
    setIsSelfCollect,
    assignDeliveryWorker: assignDeliveryWorkerAction,
  };

  return (
    <OrderDetailContext.Provider value={value}>
      {children}
    </OrderDetailContext.Provider>
  );
};

export const useOrderDetail = (): OrderDetailContextType => {
  const context = useContext(OrderDetailContext);
  if (!context) {
    throw new Error('useOrderDetail must be used within OrderDetailProvider');
  }
  return context;
};

// Utility function for formatting datetime
export const formatDateTime = (dateStr?: string): string => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString('zh-CN');
};