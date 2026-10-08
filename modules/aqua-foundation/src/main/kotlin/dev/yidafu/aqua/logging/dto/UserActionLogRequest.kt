package dev.yidafu.aqua.logging.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UserActionLogRequest(
  val userId: String? = null,
  val username: String? = null,
  @field:NotBlank val actionType: String,
  @field:NotBlank val target: String,
  val pageUrl: String? = null,
  val elementId: String? = null,
  val elementType: String? = null,
  val elementText: String? = null,
  val coordinates: CoordinatesDto? = null,
  val properties: Map<String, Any> = emptyMap(),
  val timestamp: Long = System.currentTimeMillis(),
  // 服务端补充字段(批次上报时由控制器注入,不来自客户端请求体)
  val userAgent: String? = null,
  val clientIp: String? = null,
)

data class CoordinatesDto(
  val screenX: Int,
  val screenY: Int,
  val pageX: Int,
  val pageY: Int,
)

data class BatchUserActionLogRequest(
  @field:NotNull val actions: List<BatchUserAction>,
)

data class BatchUserAction(
  val userId: String? = null,
  val username: String? = null,
  val actionType: String,
  val target: String,
  val pageUrl: String? = null,
  val elementId: String? = null,
  val elementType: String? = null,
  val elementText: String? = null,
  val coordinates: CoordinatesDto? = null,
  val properties: Map<String, Any> = emptyMap(),
  val timestamp: Long = System.currentTimeMillis(),
  var userAgent: String = "",
  var clientIp: String = "",
)
