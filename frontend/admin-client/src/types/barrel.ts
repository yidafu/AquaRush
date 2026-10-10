// Barrel types (G06 押金管理:R-MON-03 押金桶 / F-PRO-012 桶账台账)

export type DepositPriceStatus = 'ENABLED' | 'DISABLED'

export type DepositStatus = 'PENDING' | 'HELD' | 'REFUNDED'

export type DepositRefundApplicationStatus = 'PENDING' | 'RECOVERED' | 'REFUNDED' | 'REJECTED'

/** 押金价目(按规格独立配置,与商品解耦) */
export interface DepositPrice {
  id: string
  specification: string
  depositCents: number | string
  status: DepositPriceStatus
  sortOrder: number
  createdAt: string
  updatedAt: string
}

/** 押金记录(收取 → 在租 → 退还) */
export interface DepositRecord {
  id: string
  orderId: string
  specification?: string | null
  bucketCount?: number | null
  unitDepositCents?: number | string | null
  depositAmountCents: number | string
  status: DepositStatus
  collectedAt?: string | null
  refundedAt?: string | null
  refundRemark?: string | null
  createdAt: string
}

/** 退押申请(待处理 → 已回收 → 已退还 / 已驳回) */
export interface DepositRefundApplication {
  id: string
  specification: string
  bucketCount: number
  depositCents: number | string
  receiverName?: string | null
  receiverPhone?: string | null
  receiverDetail?: string | null
  remark?: string | null
  status: DepositRefundApplicationStatus
  rejectReason?: string | null
  recoveredAt?: string | null
  refundedAt?: string | null
  rejectedAt?: string | null
  createdAt: string
}

/** 桶账台账行(按客户维度,实时聚合视图) */
export interface BarrelLedgerRow {
  userId: string
  nickname?: string | null
  phone?: string | null
  heldBucketCount: number
  depositHeldCents: number | string
  depositRefundedCents: number | string
  deliveredBucketCount: number
  recoveredBucketCount: number
  owedBucketCount: number
}
