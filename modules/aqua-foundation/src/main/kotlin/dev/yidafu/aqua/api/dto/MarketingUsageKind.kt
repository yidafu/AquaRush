package dev.yidafu.aqua.api.dto

/**
 * 营销使用情况追溯方式(PRD 10-marketing.md 营销方式详情):
 * 描述某类营销方式在订单上的追溯维度,供「相关订单记录 + 营销使用情况」查询使用。
 *
 * - PROMOTION_ACTIVITY:满减/折扣活动 —— orders.promotion_activity_id = refId
 * - FIRST_ORDER_REDUCE:首单立减(订单级) —— orders.first_order_activity_id = refId
 * - COUPON_TEMPLATE:优惠券模板 —— orders.coupon_id = user_coupons.id 且 user_coupons.coupon_template_id = refId
 *   (含已退回券的订单:订单快照保留 coupon_id,口径=「曾使用过该券模板的订单」)
 * - GIFT_ACTIVITY:买赠/满赠活动 —— order_gifts.gift_activity_id = refId
 * - FLASH_SALE_ITEM:限时特价 —— order_items 匹配(商品 + 活动价 + 活动时间窗,订单未落活动 ID 快照,近似口径)
 * - NEWCOMER_PRICE_ITEM:新人专享价 —— order_items 匹配(商品 + 活动价 + 活动时间窗,同上)
 */
enum class MarketingUsageKind {
  PROMOTION_ACTIVITY,
  FIRST_ORDER_REDUCE,
  COUPON_TEMPLATE,
  GIFT_ACTIVITY,
  FLASH_SALE_ITEM,
  NEWCOMER_PRICE_ITEM,
}
