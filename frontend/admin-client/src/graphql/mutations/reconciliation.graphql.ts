import { gql } from '@apollo/client'

// 微信支付对账变更(F-ORD-031/R-PAY-04,G93;权限 PERMISSION_PAYMENT_RECONCILIATION_EXECUTE)

/**
 * 手动执行对账(创建任务并异步执行;账单日限 T-1~T-30,后端强制校验)
 */
export const EXECUTE_RECONCILIATION_MUTATION = gql`
  mutation ExecuteReconciliation($input: ExecuteReconciliationInput!) {
    executeReconciliation(input: $input) {
      id
      taskId
      taskType
      status
      taskDate
      triggerType
      errorMessage
    }
  }
`

/**
 * 差异处置(解决=RESOLVE 已核实无资金损失或已修复 / 忽略=IGNORE 跨日边界等可解释差异;
 * 备注必填,处置留痕,已处置不可再改)
 */
export const RESOLVE_DISCREPANCY_MUTATION = gql`
  mutation ResolveDiscrepancy($input: ResolveDiscrepancyInput!) {
    resolveDiscrepancy(input: $input)
  }
`
