package dev.yidafu.aqua.common.domain.model.enums

/**
 * 对账任务状态
 */
enum class ReconciliationTaskStatus {
  PENDING, // 等待执行
  RUNNING, // 执行中
  SUCCESS, // 执行成功
  FAILED, // 执行失败
}
