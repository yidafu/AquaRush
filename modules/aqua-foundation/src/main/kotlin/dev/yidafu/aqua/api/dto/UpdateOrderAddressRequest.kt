package dev.yidafu.aqua.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 送水端订单页地址信息修改请求(F-ORD-024)
 *
 * 配送员/管理员在配送任务中修正订单关联收货地址的收货人信息、定位(WGS-84 经纬度)
 * 与楼层等;仅传入字段生效,空值字段不覆盖原地址(与 UpdateAddressInput 语义一致)。
 */
data class UpdateOrderAddressRequest(
  @field:JsonProperty("receiverName")
  @field:NotBlank(message = "收货人姓名不能为空")
  @field:Size(min = 2, max = 20, message = "收货人姓名长度应在2-20个字符之间")
  val receiverName: String? = null,
  @field:JsonProperty("phone")
  @field:NotBlank(message = "收货人手机号不能为空")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String? = null,
  @field:JsonProperty("detailAddress")
  @field:NotBlank(message = "详细地址不能为空")
  @field:Size(min = 5, max = 200, message = "详细地址长度应在5-200个字符之间")
  val detailAddress: String? = null,
  @field:JsonProperty("floor")
  @field:Min(value = 1, message = "楼层必须大于0")
  val floor: Int? = null,
  @field:JsonProperty("longitude")
  @field:DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
  @field:DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
  val longitude: Double? = null,
  @field:JsonProperty("latitude")
  @field:DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
  @field:DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
  val latitude: Double? = null,
)
