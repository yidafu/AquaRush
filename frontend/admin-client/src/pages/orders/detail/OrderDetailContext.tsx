import React, { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { useQuery, useMutation } from '@apollo/client';
import { extractErrorMessage } from '@aquarush/common/utils/network';
import { message } from '@/ui';
import { ORDER_DETAIL_PAGE_DATA_QUERY } from '../../../graphql/queries/order.graphql';
import { ASSIGN_DELIVERY_WORKER_MUTATION } from '../../../graphql/mutations/order.graphql';
import type { OrderOperation } from '../../../components/OrderOperationTimeline';

export interface OrderDetailData {
  // 主键为 Snowflake Long,PrimaryId 标量在前后端边界序列化为字符串,禁止转 Number(精度丢失)
  id: string;
  orderNo: string;
  user: {
    id: string;
    nickname: string;
    phone: string;
    avatarUrl?: string;
  };
  status: string;
  // 金额(分),Long 标量序列化为字符串,展示时经 normalizeCents 转数字
  amountCents: string;
  items: Array<{
    productId: string;
    quantity: number;
    unitPriceCents?: number;
    totalPriceCents?: number;
    product?: {
      id: string;
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
    id: string;
    name: string;
    phone: string;
    avatarUrl?: string;
  };
  paymentMethod?: string;
  paymentTransactionId?: string;
  paymentTime?: string;
  paymentType?: string;
  deliveryPhotos?: string[];
  // 配送照片核验(G23):PENDING/APPROVED/REJECTED,空=未核验
  deliveryPhotoReviewStatus?: string;
  deliveryPhotoReviewedAt?: string;
  deliveryPhotoReviewedBy?: string;
  deliveryPhotoReviewRemark?: string;
  isSelfCollect: boolean;
  deliveryStartedAt?: string;
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
  id: string;
  name: string;
  phone: string;
  avatarUrl?: string;
}

// 注:订单状态文本/tone 统一来自 constants/order.ts(common ORDER_STATUS_TEXT +
// ORDER_STATUS_TONE),颜色经 constants/status.ts toneToAntDColor 桥接,勿在页面重定义。

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
  selectedWorkerId: string | null;
  isSelfCollect: boolean;

  // Actions
  loadOrder: (id: string) => void;
  refreshOrder: () => void;
  openAssignModal: () => void;
  closeAssignModal: () => void;
  setSelectedWorkerId: (id: string | null) => void;
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
  const [selectedWorkerId, setSelectedWorkerId] = useState<string | null>(null);
  const [isSelfCollect, setIsSelfCollect] = useState(false);

  // 单页单请求:订单详情 + 在线配送员 + 操作记录一次取回(规则 #10);
  // orderId 变化经 variables 变化自动重取,refreshOrder = refetch。
  // 注意:orderId 为 Snowflake Long 字符串,直接透传 PrimaryId 标量,禁止 parseInt/Number 转换(会精度丢失)。
  const { data, loading, error, refetch } = useQuery<OrderDetailQueryResult>(ORDER_DETAIL_PAGE_DATA_QUERY, {
    variables: { orderId },
    skip: !orderId,
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });

  // Mutation for assigning delivery worker
  const [assignDeliveryWorkerMutation, { loading: assignLoading }] = useMutation(ASSIGN_DELIVERY_WORKER_MUTATION);

  const order = data?.order ?? null;
  const workers: DeliveryWorker[] = data?.onlineDeliveryWorkers ?? [];
  const orderOperations: OrderOperation[] = data?.orderOperations ?? [];

  const loadOrder = useCallback((id: string) => {
    refetch({ orderId: id });
  }, [refetch]);

  const refreshOrder = useCallback(() => {
    if (orderId) {
      refetch({ orderId });
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
          orderId,
          workerId: selectedWorkerId,
          isSelfCollect,
        },
      });
      message.success('分配配送员成功');
      setAssignModalVisible(false);
      // Refresh order data
      refreshOrder();
    } catch (err: any) {
      message.error(extractErrorMessage(err, '分配配送员失败'));
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