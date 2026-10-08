package dev.yidafu.aqua.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

/**
 * 编辑水桶费价目请求(G06:规格/价格/排序可改;状态变更走 disableBarrelFee)。
 */
data class UpdateBarrelFeeRequest(
  @field:Size(max = 100, message = "规格不能超过100个字符")
  val specification: String? = null,
  @field:Min(value = 0, message = "水桶费不能小于0")
  @field:Max(value = 9999900, message = "水桶费不能大于99999元")
  val priceCents: Long? = null,
  @field:Min(value = 0, message = "排序权重不能小于0")
  @field:Max(value = 9999, message = "排序权重不能大于9999")
  val sortOrder: Int? = null,
)
