package dev.yidafu.aqua.common.domain.model.enums

/**
 * 营销适用范围(PRD 10-marketing.md):
 * - ALL: 全场
 * - PRODUCT: 指定商品(配合 productId)
 * - GROUP: 指定客户分组(配合 customerGroupId,满减/折扣活动适用)
 */
enum class CouponScope {
  ALL,
  PRODUCT,
}
