/**
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

package dev.yidafu.aqua.admin.delivery.dto

import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus

/**
 * 配送员操作输入类型
 */
data class CreateDeliveryWorkerInput(
  val wechatOpenId: String?,
  val name: String,
  val phone: String,
  val password: String?,
  val avatarUrl: String?,
  val coordinates: String?,
  val currentLocation: String?,
  val rating: Double?,
  val earningCents: Long?,
  val isAvailable: Boolean? = true,
  val accountStatus: AdminAccountStatus? = null,
)

data class UpdateDeliveryWorkerInput(
  val wechatOpenId: String?,
  val name: String?,
  val phone: String?,
  val avatarUrl: String?,
  val coordinates: String?,
  val currentLocation: String?,
  val rating: Double?,
  val earningCents: Long?,
  val isAvailable: Boolean?,
  val status: DeliverWorkerModelStatus?,
  val accountStatus: AdminAccountStatus?,
)
