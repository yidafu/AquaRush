// 订单状态文案
export const ORDER_STATUS_MAP: Record<string, string> = {
  PENDING_DISPATCH: '待接单',
  PENDING_DELIVERY: '待配送',
  DELIVERING: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
  PENDING_PAYMENT: '待支付',
};

// 配送进度步骤
export const ORDER_STEPS = ['待接单', '待配送', '配送中', '已完成'];

// 根据订单状态计算当前进度
export function getCurrentStep(status: string): number {
  const statusToStep: Record<string, number> = {
    PENDING_DISPATCH: 0,
    PENDING_DELIVERY: 1,
    DELIVERING: 2,
    COMPLETED: 3,
  };
  return statusToStep[status] ?? 0;
}

// 订单操作类型文案
export const OPERATION_TYPE_MAP: Record<string, string> = {
  ORDER_CREATED: '订单创建',
  DELIVERY_ASSIGNED: '分配配送',
  DELIVERY_STARTED: '开始配送',
  DELIVERY_COMPLETED: '配送完成',
  ORDER_COMPLETED: '订单完成',
  ORDER_CANCELLED: '订单取消',
  ORDER_PAID: '订单支付',
  PAYMENT_TIMEOUT: '支付超时',
  REFUND_COMPLETED: '退款完成',
};

// 收款方式文案
export const PAYMENT_TYPE_LABELS: Record<string, string> = {
  CASH: '现金',
  QR_CODE: '二维码',
  WATER_TICKET: '水票',
};

export function getPaymentTypeLabel(paymentType?: string): string {
  if (!paymentType) return '未付款';
  return PAYMENT_TYPE_LABELS[paymentType] ?? paymentType;
}

export function formatPaymentStatus(isSelfCollect: boolean, paymentType?: string): string {
  if (isSelfCollect) return '已收款';
  return getPaymentTypeLabel(paymentType);
}
