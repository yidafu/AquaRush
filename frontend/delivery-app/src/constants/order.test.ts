/**
 * 订单常量与状态工具函数单测(纯业务逻辑,无 UI)
 * 覆盖:状态文案映射、配送进度步骤、收款方式文案、支付状态展示。
 */
import {
  formatPaymentStatus,
  getCurrentStep,
  getPaymentTypeLabel,
  OPERATION_TYPE_MAP,
  ORDER_STATUS_MAP,
  ORDER_STEPS,
  PAYMENT_TYPE_LABELS,
} from '@/constants/order';

describe('order 常量', () => {
  it('ORDER_STATUS_MAP 覆盖全部订单状态文案', () => {
    expect(ORDER_STATUS_MAP).toEqual({
      PENDING_DISPATCH: '待接单',
      PENDING_DELIVERY: '待配送',
      DELIVERING: '配送中',
      COMPLETED: '已完成',
      CANCELLED: '已取消',
      REFUNDED: '已退款',
      PENDING_PAYMENT: '待支付',
    });
  });

  it('ORDER_STEPS 配送进度顺序正确', () => {
    expect(ORDER_STEPS).toEqual(['待接单', '待配送', '配送中', '已完成']);
  });

  it('OPERATION_TYPE_MAP / PAYMENT_TYPE_LABELS 文案完整', () => {
    expect(OPERATION_TYPE_MAP.ORDER_COMPLETED).toBe('订单完成');
    expect(OPERATION_TYPE_MAP.PAYMENT_TIMEOUT).toBe('支付超时');
    expect(PAYMENT_TYPE_LABELS).toEqual({
      CASH: '现金',
      QR_CODE: '二维码',
      WATER_TICKET: '水票',
    });
  });
});

describe('getCurrentStep', () => {
  it('已知状态映射到对应进度步骤', () => {
    expect(getCurrentStep('PENDING_DISPATCH')).toBe(0);
    expect(getCurrentStep('PENDING_DELIVERY')).toBe(1);
    expect(getCurrentStep('DELIVERING')).toBe(2);
    expect(getCurrentStep('COMPLETED')).toBe(3);
  });

  it('未知/异常状态回退到第 0 步', () => {
    expect(getCurrentStep('CANCELLED')).toBe(0);
    expect(getCurrentStep('REFUNDED')).toBe(0);
    expect(getCurrentStep('')).toBe(0);
  });
});

describe('getPaymentTypeLabel', () => {
  it('已知收款方式返回中文文案', () => {
    expect(getPaymentTypeLabel('CASH')).toBe('现金');
    expect(getPaymentTypeLabel('QR_CODE')).toBe('二维码');
    expect(getPaymentTypeLabel('WATER_TICKET')).toBe('水票');
  });

  it('未知类型原样返回', () => {
    expect(getPaymentTypeLabel('BANK_TRANSFER')).toBe('BANK_TRANSFER');
  });

  it('空值返回「未付款」', () => {
    expect(getPaymentTypeLabel()).toBe('未付款');
    expect(getPaymentTypeLabel('')).toBe('未付款');
  });
});

describe('formatPaymentStatus', () => {
  it('自提订单显示「已收款」', () => {
    expect(formatPaymentStatus(true, 'CASH')).toBe('已收款');
    expect(formatPaymentStatus(true)).toBe('已收款');
  });

  it('非自提订单按收款方式展示', () => {
    expect(formatPaymentStatus(false, 'QR_CODE')).toBe('二维码');
    expect(formatPaymentStatus(false)).toBe('未付款');
  });
});
