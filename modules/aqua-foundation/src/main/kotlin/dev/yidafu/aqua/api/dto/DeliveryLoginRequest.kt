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
