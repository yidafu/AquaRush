package dev.yidafu.aqua.api.dto

/**
 * 营销活动名称解析引用(F-STA-007 营销统计明细,查询侧批量取名):
 * kind 为 MarketingBreakdownKind 的 name(COUPON_TEMPLATE/PROMOTION_ACTIVITY/FIRST_ORDER_ACTIVITY/GIFT_ACTIVITY/TICKET_TYPE),
 * refId 为活动/券模板/票种 ID。以 String 承载 kind 保持跨模块解耦(不引用 analytics 枚举)。
 */
data class MarketingActivityNameRef(
  val kind: String,
  val refId: Long,
)
