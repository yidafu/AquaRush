package dev.yidafu.aqua.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * 购桶明细项请求(G06:一次性塑料水桶费,独立支付)。
 */
data class PurchaseBarrelItemRequest(
  @field:NotNull(message = "水桶费价目不能为空")
  val barrelFeeId: Long,
  @field:NotNull(message = "购桶数量不能为空")
  @field:Min(value = 1, message = "购桶数量必须大于0")
  @field:Max(value = 99, message = "单次购桶数量不能超过99")
  val quantity: Int,
)
