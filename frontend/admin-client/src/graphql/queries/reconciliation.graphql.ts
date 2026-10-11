import { gql } from '@apollo/client'

// 微信支付对账(F-ORD-031/R-PAY-04,G93;权限 PERMISSION_PAYMENT_RECONCILIATION_READ)

/**
 * 任务列表分页查询(单页单请求,规则 #7:一次渲染一个 GraphQL 请求;
 * 任务行自带微信/本地两侧汇总列,列表页无需再取报表)
 */
export const RECONCILIATION_TASKS_PAGE_QUERY = gql`
  query ReconciliationTasksPage(
    $taskType: ReconciliationTaskType
    $status: ReconciliationTaskStatus
    $dateFrom: String
    $dateTo: String
    $page: Int
    $size: Int
  ) {
    reconciliationTasks(taskType: $taskType, status: $status, dateFrom: $dateFrom, dateTo: $dateTo, page: $page, size: $size) {
      content {
        id
        taskId
        taskType
        status
        taskDate
        startTime
        endTime
        totalRecords
        matchedRecords
        unmatchedRecords
        errorMessage
        triggerType
        executedBy
        wechatTotalCount
        wechatTotalAmountCents
        localTotalCount
        localTotalAmountCents
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`

/** ReconciliationTasksPage 操作名(供 refetchQueries 引用) */
export const RECONCILIATION_TASKS_PAGE = 'ReconciliationTasksPage'

/**
 * 任务详情下钻:报表汇总 + 差异首页 —— 单请求合并两个 root field(规则 #7,不链式串行)
 */
export const RECONCILIATION_DETAIL_QUERY = gql`
  query ReconciliationDetail($taskId: String!, $page: Int, $size: Int) {
    reconciliationReports(taskId: $taskId) {
      id
      reportType
      reportData
      generatedAt
    }
    reconciliationDiscrepancies(taskId: $taskId, page: $page, size: $size) {
      content {
        id
        taskId
        discrepancyType
        sourceSystem
        recordId
        recordDetails
        status
        resolutionNotes
        resolvedBy
        resolvedAt
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`

/** ReconciliationDetail 操作名(供 refetchQueries 引用) */
export const RECONCILIATION_DETAIL = 'ReconciliationDetail'
