package dev.yidafu.aqua.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 创建订单请求(F-ORD-004 多商品下单,PRD 04-order.md):
 * items 为商品明细列表(1~20 项,同一商品不可重复),库存逐项校验、金额一律服务端计算(R-MON-01);
 * 单商品下单=items 单元素;代客下单由 CreateDeliveryOrderInput 映射为 items。
 */
data class CreateOrderRequest(
  @field:JsonProperty("userId")
  @field:NotNull(message = "用户ID不能为空")
  val userId: Long,
  @field:JsonProperty("items")
  @field:Valid
  @field:NotEmpty(message = "订单商品明细不能为空")
  @field:Size(min = 1, max = 20, message = "订单商品明细数量需在1-20项之间")
  val items: List<OrderItemRequest>,
  @field:JsonProperty("addressId")
  @field:NotNull(message = "配送地址ID不能为空")
  val addressId: Long,
  @field:JsonProperty("remark")
  @field:Size(max = 500, message = "备注长度不能超过500个字符")
  val remark: String? = null,
  val isSelfCollect: Boolean? = false,
  // 服务端下单幂等键(R-ORD-06):同一用户同一 clientRequestId 重复提交不产生重复订单;null=不启用幂等
  @field:Size(max = 64, message = "clientRequestId长度不能超过64个字符")
  val clientRequestId: String? = null,
  // 配送时段(AC-ORD-001-05,下单时快照落库;null=默认立即配送 IMMEDIATE)
  val deliveryTimeSlot: DeliveryTimeSlot? = null,
  // 优惠券选择(促销引擎 R-MKT-07 叠加③):null=引擎自动选最优,0=不用券;一单一券
  @field:Min(value = 0, message = "优惠券ID无效")
  val preferredCouponId: Long? = null,
  // 水票核销张数(一票抵一桶 R-MKT-08,≤下单桶数 ∩ 可用张数)
  @field:Min(value = 0, message = "水票核销张数无效")
  @field:Max(value = 999, message = "水票核销张数不能超过999")
  val ticketRedeemCount: Int = 0,
)

/**
 * 订单商品明细项(单商品=单元素列表;同一商品不可重复出现,由服务端校验)
 */
data class OrderItemRequest(
  @field:JsonProperty("productId")
  @field:NotNull(message = "商品ID不能为空")
  val productId: Long,
  @field:JsonProperty("quantity")
  @field:NotNull(message = "数量不能为空")
  @field:Min(value = 1, message = "数量必须大于0")
  @field:Max(value = 999, message = "数量不能超过999")
  val quantity: Int,
)
