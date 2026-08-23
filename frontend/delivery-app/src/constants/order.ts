// 订单状态文案/进度步骤/状态步骤工具 —— 统一从 common 取(单来源,见 common/utils/order-status)
export { ORDER_STATUS_TEXT, ORDER_STEPS, getCurrentStep } from '@aquarush/common/utils/order-status';

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
