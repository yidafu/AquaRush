package dev.yidafu.aqua.api.dto

import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 买赠/满赠活动创建请求(PRD 10-marketing.md §F-MKT-008/R-MKT-09):
 * 满赠=订单现金部分商品小计满 X 元赠 Y 桶;买赠=指定商品满 X 桶送 Y 桶;v1 单档单赠品。
 */
data class GiftActivityCreateRequest(
  @field:NotBlank(message = "活动名称不能为空")
  @field:Size(max = 100, message = "活动名称不能超过100个字")
  val name: String,
  @field:NotNull(message = "活动类型不能为空")
  val giftType: GiftActivityType,
  val thresholdCents: Long? = null,
  val thresholdQuantity: Int? = null,
  @field:NotNull(message = "适用范围不能为空")
  val scope: PromotionScope,
  val productId: Long? = null,
  val customerGroupId: Long? = null,
  @field:NotNull(message = "赠品商品不能为空")
  val giftProductId: Long,
  @field:NotNull(message = "赠品数量不能为空")
  @field:Min(value = 1, message = "赠品数量无效")
  val giftQuantity: Int,
  @field:NotNull(message = "开始时间不能为空")
  val validFrom: LocalDateTime,
  @field:NotNull(message = "结束时间不能为空")
  val validTo: LocalDateTime,
)
