package dev.yidafu.aqua.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/**
 * 购桶请求(G06:一次性塑料水桶费,独立支付,每用户总限购一次)。
 */
data class PurchaseBarrelRequest(
  @field:NotEmpty(message = "购桶明细不能为空")
  @field:Size(min = 1, max = 20, message = "购桶明细数量应在1-20之间")
  @field:Valid
  val items: List<PurchaseBarrelItemRequest>,
)
