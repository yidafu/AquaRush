package dev.yidafu.aqua.api.query

import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 创建配送员请求数据传输对象
 */
data class CreateDeliveryWorkerRequest(
  @field:NotBlank(message = "姓名不能为空")
  @field:Size(max = 50, message = "姓名长度不能超过50个字符")
  val name: String,
  @field:NotBlank(message = "手机号不能为空")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String,
  @field:NotNull(message = "配送区域ID不能为空")
  val deliveryAreaId: Long,
  @field:Size(max = 500, message = "备注长度不能超过500个字符")
  val notes: String? = null,
  @field:NotNull(message = "状态不能为空")
  val status: DeliverWorkerModelStatus = DeliverWorkerModelStatus.OFFLINE,
)
