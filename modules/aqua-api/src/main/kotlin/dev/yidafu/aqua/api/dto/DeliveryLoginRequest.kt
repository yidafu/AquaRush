package dev.yidafu.aqua.api.dto

/**
 * Request DTO for delivery worker login
 */
data class DeliveryLoginRequest(
  val code: String,
  val phoneNumber: String? = null,
  val encryptedData: String? = null,
  val iv: String? = null,
)

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
  val wechatOpenId: String,
  val role: String,
)
