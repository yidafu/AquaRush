/*
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.delivery.service.dto

import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel

/**
 * Request DTO for delivery worker login
 */
data class DeliveryLoginRequest(
  val code: String,
  val phoneNumber: String? = null,
  val encryptedData: String? = null,
  val iv: String? = null
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
  val openId: String? = null
)

/**
 * Delivery worker info DTO
 */
data class DeliveryWorkerInfo(
  val id: Long,
  val name: String,
  val phone: String,
  val avatarUrl: String?,
  val wechatOpenId: String
)

/**
 * Extension function to convert DeliveryWorkerModel to DeliveryWorkerInfo
 */
fun DeliveryWorkerModel.toDeliveryWorkerInfo(): DeliveryWorkerInfo = DeliveryWorkerInfo(
  id = id!!,
  name = name,
  phone = phone,
  avatarUrl = avatarUrl,
  wechatOpenId = wechatOpenId
)
