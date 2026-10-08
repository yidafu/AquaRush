package dev.yidafu.aqua.api.dto

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
