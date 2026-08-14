package dev.yidafu.aqua.common.domain.model.enums

/**
 * 满减/折扣活动存储状态:
 * - ACTIVE: 启用(配合时间窗判定进行中/未开始/已结束)
 * - DISABLED: 停用(立即失效,不再作用于新订单)
 */
enum class PromotionActivityStatus {
  ACTIVE,
  DISABLED,
}
