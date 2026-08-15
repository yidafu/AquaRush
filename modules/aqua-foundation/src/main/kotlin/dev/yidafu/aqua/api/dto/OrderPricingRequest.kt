package dev.yidafu.aqua.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 促销引擎计价请求(PRD 10-marketing.md §R-MKT-02/07/08):
 * items 商品明细列表(1~20 项),成交单价按 R-MKT-02 逐商品解析(分组价→目录价),
 * 订单层按 R-MKT-07 叠加(满减→优惠券),水票核销先于现金计价;
 * 金额一律服务端计算(R-MON-01),前端不传价格。
 */
data class OrderPricingRequest(
  @field:NotNull(message = "用户不能为空")
  val userId: Long,
  @field:Valid
  @field:NotEmpty(message = "商品明细不能为空")
  @field:Size(min = 1, max = 20, message = "商品明细数量需在1-20项之间")
  val items: List<OrderPricingItemRequest>,
  val isSelfCollect: Boolean = false,
  // 优惠券偏好(null=引擎自动选最优,0=不用券;一单一券 R-MKT-03)
  val preferredCouponId: Long? = null,
  // 水票核销张数(≤下单总桶数 ∩ 可用张数;被核销桶按单价 0 计入 R-MKT-08)
  @field:Min(value = 0, message = "水票核销张数无效")
  @field:Max(value = 999, message = "水票核销张数不能超过999")
  val ticketRedeemCount: Int = 0,
)

/**
 * 计价商品明细项(同一商品不可重复出现,由引擎校验)
 */
data class OrderPricingItemRequest(
  @field:NotNull(message = "商品不能为空")
  val productId: Long,
  @field:NotNull(message = "数量不能为空")
  @field:Min(value = 1, message = "数量必须大于0")
  @field:Max(value = 999, message = "数量不能超过999")
  val quantity: Int,
)
