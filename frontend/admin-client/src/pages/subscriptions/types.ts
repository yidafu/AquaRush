/**
 * 周期订购计划列表页类型定义(pages/subscriptions 拆分):
 * 表格列渲染、新建弹窗与页面共享,避免组件与页面循环依赖。
 */

export interface SubscriptionItemData {
  productId: string;
  productName: string;
  quantity: number;
}

export interface SubscriptionData {
  // PrimaryId 序列化为字符串(后端 Long Snowflake)
  id: string;
  user: {
    id: string;
    nickname: string;
    phone: string;
  } | null;
  address: {
    receiverName: string;
    phone: string;
    province: string;
    city: string;
    district: string;
    detailAddress: string;
  } | null;
  workerId: string | null;
  workerName: string | null;
  cycleType: string;
  weekdays: number[];
  monthDay: number | null;
  timeSlot: string;
  startDate: string;
  endDate: string | null;
  nextRunDate: string;
  maxPeriods: number | null;
  executedPeriods: number;
  status: string;
  remark: string | null;
  /** 最近一次到期执行失败原因(非空时行内警示,不静默失败) */
  lastError: string | null;
  lastRunAt: string | null;
  items: SubscriptionItemData[];
  createdAt: string;
}

export interface SubscriptionWorkerOption {
  id: string;
  name: string;
  phone: string;
}

export interface SubscriptionsQueryResult {
  subscriptions: {
    content: SubscriptionData[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  };
  deliveryWorkers: {
    content: SubscriptionWorkerOption[];
    totalElements: number;
  };
}

export interface SubscriptionAddressesQueryResult {
  userAddresses: {
    id: string;
    receiverName: string;
    phone: string;
    province: string;
    city: string;
    district: string;
    detailAddress: string;
    isDefault: boolean;
  }[];
}
