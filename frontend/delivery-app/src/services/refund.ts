import { networkManager } from '@/utils/network-manager';
import type { ProcessRefundInput, RefundRequestVo, RefundRequestVoPage, RefundStatus } from '@aquarush/common';

// ==================== 退单(退款)处置(R-PAY-02) ====================

/**
 * 退款申请分页查询(管理员,PERMISSION_PAYMENT_READ)。
 * 服务端分页(规则 #13),支持状态筛选。
 */
export const getRefundRequests = (status?: RefundStatus, page = 0, size = 10) => {
  return networkManager.query<{ refundRequests: RefundRequestVoPage }>(
    `
    query GetRefundRequests($status: RefundStatus, $page: Int, $size: Int) {
      refundRequests(status: $status, page: $page, size: $size) {
        content {
          id
          originalTransactionId
          orderId
          userId
          amountCents
          reason
          status
          requestedAt
          processedAt
          processedBy
          adminNote
        }
        totalElements
        totalPages
        size
        number
      }
    }
  `,
    { status: status || null, page, size },
  );
};

/**
 * 审核退款申请(管理员,PERMISSION_PAYMENT_REFUND):
 * APPROVE = 执行微信/假退款并流转订单「已退款」;REJECT = 驳回终止;FAILED 记录可重试(APPROVE)。
 */
export const processRefund = (input: ProcessRefundInput) => {
  return networkManager.mutate<{ processRefund: RefundRequestVo }>(
    `
    mutation ProcessRefund($input: ProcessRefundInput!) {
      processRefund(input: $input) {
        id
        orderId
        userId
        amountCents
        reason
        status
        requestedAt
        processedAt
        processedBy
        adminNote
      }
    }
  `,
    { input },
  );
};

export type { ProcessRefundInput, RefundRequestVo, RefundRequestVoPage, RefundStatus };
