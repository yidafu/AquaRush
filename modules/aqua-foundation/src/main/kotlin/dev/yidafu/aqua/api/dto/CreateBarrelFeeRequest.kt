package dev.yidafu.aqua.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 新增水桶费价目请求(G06:独立价目表,按规格配置,与商品解耦)。
 */
data class CreateBarrelFeeRequest(
  @field:NotBlank(message = "规格不能为空")
  @field:Size(max = 100, message = "规格不能超过100个字符")
  val specification: String,
  @field:Min(value = 0, message = "水桶费不能小于0")
  @field:Max(value = 9999900, message = "水桶费不能大于99999元")
  val priceCents: Long,
  @field:Min(value = 0, message = "排序权重不能小于0")
  @field:Max(value = 9999, message = "排序权重不能大于9999")
  val sortOrder: Int = 999,
)
