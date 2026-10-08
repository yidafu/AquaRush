package dev.yidafu.aqua.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 限时特价活动创建请求(PRD 10-marketing.md §F-MKT-005/R-MKT-05):
 * 特价 0.01-99999 元、≤2 位小数(以分存储);特价低于目录价(上浮为开放问题,默认不允许)。
 */
data class FlashSaleActivityCreateRequest(
  @field:NotBlank(message = "活动名称不能为空")
  @field:Size(max = 100, message = "活动名称不能超过100个字")
  val name: String,
  @field:NotNull(message = "商品不能为空")
  val productId: Long,
  @field:NotNull(message = "特价不能为空")
  @field:Min(value = 1, message = "特价无效")
  @field:Max(value = 9_999_900, message = "特价无效")
  val salePriceCents: Long,
  @field:NotNull(message = "开始时间不能为空")
  val validFrom: LocalDateTime,
  @field:NotNull(message = "结束时间不能为空")
  val validTo: LocalDateTime,
)
