package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 首单立减/新人专享价活动更新请求(PRD 10-marketing.md §F-MKT-009/R-MKT-10)
 */
data class FirstOrderActivityUpdateRequest(
  @field:NotBlank(message = "活动名称不能为空")
  @field:Size(max = 100, message = "活动名称不能超过100个字")
  val name: String,
  @field:NotNull(message = "活动类型不能为空")
  val activityType: FirstOrderActivityType,
  val thresholdCents: Long? = null,
  val discountAmountCents: Long? = null,
  @field:Min(value = 100, message = "折扣率超出范围")
  @field:Max(value = 9_900, message = "折扣率超出范围")
  val discountRatePermille: Long? = null,
  @field:NotNull(message = "适用范围不能为空")
  val scope: PromotionScope,
  val productId: Long? = null,
  val newcomerPriceCents: Long? = null,
  @field:NotNull(message = "开始时间不能为空")
  val validFrom: LocalDateTime,
  @field:NotNull(message = "结束时间不能为空")
  val validTo: LocalDateTime,
)
