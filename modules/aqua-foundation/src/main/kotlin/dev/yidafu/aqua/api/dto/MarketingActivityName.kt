package dev.yidafu.aqua.api.dto

/**
 * 营销活动名称解析结果(F-STA-007 营销统计明细):
 * - name:活动/券模板/票种名称(未命中为空字符串)
 * - giftProductId:仅 GIFT_ACTIVITY 有效(赠品商品 ID,供让利估算 = 赠品件数 × 当前目录价);其余 kind 为 null
 */
data class MarketingActivityName(
  val name: String,
  val giftProductId: Long? = null,
)
