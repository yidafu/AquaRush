// Barrel types (G06 水桶费:一次性塑料水桶费,独立价目表 + 购桶单)

export type BarrelFeeStatus = 'ENABLED' | 'DISABLED'

export type BarrelPaymentStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED'

export interface BarrelFee {
  id: string
  specification: string
  priceCents: number | string
  status: BarrelFeeStatus
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface BarrelPurchaseItem {
  specification: string
  unitPriceCents: number | string
  quantity: number
  amountCents: number | string
}

export interface BarrelPurchase {
  id: string
  orderNo: string
  userId: string
  totalAmountCents: number | string
  paymentStatus: BarrelPaymentStatus
  paidAt?: string
  refundedAt?: string
  refundedBy?: string
  refundRemark?: string
  remark?: string
  items: BarrelPurchaseItem[]
  createdAt: string
  updatedAt: string
}
