package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.OrderModel
import org.springframework.data.domain.Page

/**
 * 营销使用情况查询结果(PRD 10-marketing.md 营销方式详情):
 * summary 为使用汇总,orders 为相关订单分页(按下单时间倒序)。
 */
data class MarketingUsagePage(
  val summary: MarketingUsageSummary = MarketingUsageSummary(),
  val orders: Page<OrderModel> = Page.empty(),
)
