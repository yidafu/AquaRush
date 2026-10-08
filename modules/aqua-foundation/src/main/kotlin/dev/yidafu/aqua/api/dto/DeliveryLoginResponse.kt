package dev.yidafu.aqua.api.dto

/**
 * Response DTO for delivery worker login
 */
data class DeliveryLoginResponse(
  val token: String?,
  val refreshToken: String?,
  val needBindPhone: Boolean,
  val workerInfo: DeliveryWorkerInfo?,
  val message: String?,
)
