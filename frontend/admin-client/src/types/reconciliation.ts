/**
 * 微信支付对账(F-ORD-031/R-PAY-04,G93)页面类型
 * 对应 shared-types.graphqls §微信资金对账域;金额一律分(number,展示层经 displayCents 格式化)。
 */

/** 对账任务类型 */
export type ReconciliationTaskType = 'PAYMENT' | 'REFUND' | 'SETTLEMENT'

/** 对账任务状态(SKIPPED:未配置真实商户参数,假支付模式跳过) */
export type ReconciliationTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'

/** 触发方式 */
export type ReconciliationTriggerType = 'SCHEDULED' | 'MANUAL'

/** 差异类型(MISSING+WECHAT=短款 / MISSING+INTERNAL=长款,语义映射见 helpers.discrepancyLabel) */
export type DiscrepancyType = 'MISSING' | 'MISMATCH' | 'EXTRA'

/** 差异处理状态 */
export type DiscrepancyStatus = 'UNRESOLVED' | 'RESOLVED' | 'IGNORED'

/** 差异来源系统 */
export type SourceSystem = 'INTERNAL' | 'WECHAT'

/** 处置动作(解决/忽略,备注必填) */
export type DiscrepancyResolveAction = 'RESOLVE' | 'IGNORE'

/** 对账差异 recordDetails 金额字段(JsonObject 透传,分) */
export interface DiscrepancyAmounts {
  wechat?: {
    amountCents?: number | string | null
    refundFeeCents?: number | string | null
  } | null
  internal?: {
    amountCents?: number | string | null
  } | null
  amountCents?: number | string | null
  refundFeeCents?: number | string | null
  [key: string]: unknown
}

/** 对账任务 */
export interface ReconciliationTask {
  id: string
  taskId: string
  taskType: ReconciliationTaskType
  status: ReconciliationTaskStatus
  taskDate: string
  startTime: string | null
  endTime: string | null
  totalRecords: number
  matchedRecords: number
  unmatchedRecords: number
  errorMessage: string | null
  triggerType: ReconciliationTriggerType
  executedBy: string | null
  /** 微信账单侧总笔数(三方校验) */
  wechatTotalCount: number
  /** 微信账单侧总金额(分) */
  wechatTotalAmountCents: number | string
  /** 本地账实侧总笔数 */
  localTotalCount: number
  /** 本地账实侧总金额(分) */
  localTotalAmountCents: number | string
  createdAt: string
  updatedAt: string
}

export interface ReconciliationTaskPage {
  content: ReconciliationTask[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

/** 对账差异 */
export interface ReconciliationDiscrepancy {
  id: string
  taskId: string
  discrepancyType: DiscrepancyType
  sourceSystem: SourceSystem
  recordId: string
  recordDetails: DiscrepancyAmounts | null
  status: DiscrepancyStatus
  resolutionNotes: string | null
  resolvedBy: string | null
  resolvedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ReconciliationDiscrepancyPage {
  content: ReconciliationDiscrepancy[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

/** 对账报表(SUMMARY/DETAIL,reportData 为 JSON) */
export interface ReconciliationReport {
  id: string
  taskId: string
  reportType: string
  reportData: Record<string, unknown>
  fileName: string | null
  filePath: string | null
  fileSize: number | null
  generatedAt: string
}
