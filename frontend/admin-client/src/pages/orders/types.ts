/**
 * 订单列表页类型定义(orders/index.tsx 拆分时抽出):
 * 表格列渲染、批量派单弹窗与页面共享,避免组件与页面循环依赖。
 */

export interface OrderData {
  // PrimaryId 序列化为字符串(后端 Long Snowflake)
  id: string;
  orderNo: string;
  user: {
    id: string;
    nickname: string;
    phone: string;
  };
  status: string;
  amount: number;
  address: {
    receiverName: string;
    phone: string;
    province: string;
    city: string;
    district: string;
    detailAddress: string;
  };
  deliveryWorker: {
    id: string;
    name: string;
    phone: string;
  } | null;
  createdAt: string;
  updatedAt: string;
}

export interface DeliveryWorkerOption {
  id: string;
  name: string;
  phone: string;
  onlineStatus: string;
  currentTaskCount?: number;
}

export interface OrdersQueryResult {
  orders: {
    content: OrderData[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  };
  onlineDeliveryWorkers: DeliveryWorkerOption[];
}
