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

/**
 * Delivery worker info DTO
 */
data class DeliveryWorkerInfo(
  val id: Long,
  val name: String,
  val phone: String,
  val avatarUrl: String?,
//  val wechatOpenId: String,
  val role: String,
)
