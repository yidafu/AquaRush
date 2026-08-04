package dev.yidafu.aqua.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateOrderRequest(
  @field:JsonProperty("userId")
  @field:NotNull(message = "用户ID不能为空")
  val userId: Long,
  @field:JsonProperty("productId")
  @field:NotNull(message = "产品ID不能为空")
  val productId: Long,
  @field:JsonProperty("quantity")
  @field:NotNull(message = "数量不能为空")
  @field:Min(value = 1, message = "数量必须大于0")
  val quantity: Int,
  @field:JsonProperty("addressId")
  @field:NotNull(message = "配送地址ID不能为空")
  val addressId: Long,
  @field:JsonProperty("remark")
  @field:Size(max = 500, message = "备注长度不能超过500个字符")
  val remark: String? = null,
  val isSelfCollect: Boolean? = false,
)
