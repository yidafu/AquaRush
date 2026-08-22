package dev.yidafu.aqua.api.service.marketing

import dev.yidafu.aqua.api.dto.MarketingActivityName
import dev.yidafu.aqua.api.dto.MarketingActivityNameRef
import dev.yidafu.aqua.api.dto.MarketingUsagePage

/**
 * 营销使用情况查询服务(PRD 10-marketing.md 营销方式详情):
 * 返回「营销使用汇总 + 相关订单分页」;营销方式不存在时返回空汇总(0)与空分页。
 * 口径见 MarketingUsageKind/MarketingUsageSummary;由 aqua-marketing 模块实现。
 */
interface MarketingUsageQueryService {
  /**
   * 批量解析营销活动/券模板/票种名称(F-STA-007 营销统计明细,查询侧取名,中间表不落 name 列):
   * kind 为 MarketingBreakdownKind 的 name;GIFT_ACTIVITY 行附带赠品商品 ID 供让利估算;
   * 未命中的引用返回空名(展示为「已删除」)。
   */
  fun resolveMarketingActivityNames(refs: List<MarketingActivityNameRef>): Map<MarketingActivityNameRef, MarketingActivityName>

  /**
   * 优惠券模板使用情况(F-MKT-003):orders.coupon_id = user_coupons.id 且 coupon_template_id = templateId;
   * benefitCents = Σ coupon_discount_cents;usageCount = 已核销券张数。
   */
  fun couponTemplateUsage(
    templateId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage

  /**
   * 满减/折扣活动使用情况(F-MKT-004):orders.promotion_activity_id = activityId;
   * benefitCents = Σ promotion_discount_cents;usageCount = 使用订单数。
   */
  fun promotionActivityUsage(
    activityId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage

  /**
   * 限时特价活动使用情况(F-MKT-005):order_items 按「商品+活动价+时间窗」匹配(近似口径);
   * benefitCents = Σ(当前目录价 − 成交单价) × 数量;usageCount = 命中商品件数。
   */
  fun flashSaleActivityUsage(
    activityId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage

  /**
   * 买赠/满赠活动使用情况(F-MKT-008):order_gifts.gift_activity_id = activityId;
   * benefitCents = Σ 赠品数量 × 赠品当前目录价(估算);usageCount = 赠品总件数。
   */
  fun giftActivityUsage(
    activityId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage

  /**
   * 首单立减/新人专享价活动使用情况(F-MKT-009),按活动 activityType 分发:
   * FIRST_ORDER_REDUCE → orders.first_order_activity_id = activityId(立减金额合计);
   * NEWCOMER_PRICE → order_items 按「商品+新人价+时间窗」匹配(让利金额估算)。
   */
  fun firstOrderActivityUsage(
    activityId: Long,
    page: Int,
    size: Int,
  ): MarketingUsagePage
}
