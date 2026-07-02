package dev.yidafu.aqua.logging.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UserActionLogRequest(
  val userId: String? = null,
  val username: String? = null,
  @field:NotBlank val actionType: String,
  @field:NotBlank val target: String,
  val coordinates: CoordinatesDto? = null,
  val properties: Map<String, Any> = emptyMap(),
  val timestamp: Long = System.currentTimeMillis(),
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
  val coordinates: CoordinatesDto? = null,
  val properties: Map<String, Any> = emptyMap(),
  val timestamp: Long = System.currentTimeMillis(),
  var userAgent: String = "",
  var clientIp: String = "",
)