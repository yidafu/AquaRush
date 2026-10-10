// Refund types (R-PAY-02 管理员退款审核流)

export type RefundStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'FAILED'

export interface RefundRequest {
  // 退款请求ID(Long Snowflake 序列化为字符串)
  id: string
  originalTransactionId: string
  orderId: string
  userId: string
  amountCents: number | string
  reason: string
  status: RefundStatus
  requestedAt: string
  processedAt?: string
  processedBy?: string
  adminNote?: string
}

export interface RefundRequestVoPage {
  content: RefundRequest[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
