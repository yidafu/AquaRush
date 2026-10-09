/**
 * AquaRush Admin User Resolver DTOs
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

package dev.yidafu.aqua.admin.user.dto

import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.graphql.generated.CreateAdminInput
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateAdminInput
import dev.yidafu.aqua.user.mapper.toDomain
import dev.yidafu.aqua.common.graphql.generated.AdminAccountStatus as AdminAccountStatusG

/**
 * Request DTO for creating admin user
 *
 * 送水员档案并入 admins 单表后,创建 DELIVERY_WORKER 角色账号时 username 可空,
 * 后端自动生成 delivery_{账号ID}(见 F-USR-021);配送字段(头像/在线状态/接单权限)一并提交。
 */
data class CreateAdminRequest(
  val username: String?,
  val password: String?,
  val realName: String?,
  val phone: String?,
  val role: AdminRoleModel,
  val avatarUrl: String?,
  val onlineStatus: DeliverWorkerModelStatus,
  val isAvailable: Boolean,
  val accountStatus: AdminAccountStatus,
) {
  companion object {
    fun fromInput(input: CreateAdminInput): CreateAdminRequest =
      CreateAdminRequest(
        username = input.username,
        password = input.password,
        realName = input.realName,
        phone = input.phone,
        role = input.role.toDomain(),
        avatarUrl = input.avatarUrl,
        onlineStatus = input.status?.let { mapStatus(it) } ?: DeliverWorkerModelStatus.OFFLINE,
        isAvailable = input.isAvailable ?: true,
        accountStatus = input.accountStatus?.let { mapAccountStatus(it) } ?: AdminAccountStatus.ACTIVE,
      )

    private fun mapStatus(status: DeliveryWorkerStatus): DeliverWorkerModelStatus =
      when (status) {
        DeliveryWorkerStatus.ONLINE -> DeliverWorkerModelStatus.ONLINE
        DeliveryWorkerStatus.OFFLINE -> DeliverWorkerModelStatus.OFFLINE
      }

    private fun mapAccountStatus(status: AdminAccountStatusG): AdminAccountStatus =
      when (status) {
        AdminAccountStatusG.ACTIVE -> AdminAccountStatus.ACTIVE
        AdminAccountStatusG.DISABLED -> AdminAccountStatus.DISABLED
      }
  }
}

/**
 * Request DTO for updating admin user
 */
data class UpdateAdminRequest(
  val realName: String?,
  val phone: String?,
  val role: AdminRoleModel?,
  val avatarUrl: String?,
  val onlineStatus: DeliverWorkerModelStatus?,
  val isAvailable: Boolean?,
  val accountStatus: AdminAccountStatus?,
) {
  companion object {
    fun fromInput(input: UpdateAdminInput): UpdateAdminRequest =
      UpdateAdminRequest(
        realName = input.realName,
        phone = input.phone,
        role = input.role?.toDomain(),
        avatarUrl = input.avatarUrl,
        onlineStatus = input.status?.let { mapStatus(it) },
        isAvailable = input.isAvailable,
        accountStatus = input.accountStatus?.let { mapAccountStatus(it) },
      )

    private fun mapStatus(status: DeliveryWorkerStatus): DeliverWorkerModelStatus =
      when (status) {
        DeliveryWorkerStatus.ONLINE -> DeliverWorkerModelStatus.ONLINE
        DeliveryWorkerStatus.OFFLINE -> DeliverWorkerModelStatus.OFFLINE
      }

    private fun mapAccountStatus(status: AdminAccountStatusG): AdminAccountStatus =
      when (status) {
        AdminAccountStatusG.ACTIVE -> AdminAccountStatus.ACTIVE
        AdminAccountStatusG.DISABLED -> AdminAccountStatus.DISABLED
      }
  }
}
