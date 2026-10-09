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

package dev.yidafu.aqua.delivery.mapper

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.common.graphql.generated.AdminAccountStatus
import dev.yidafu.aqua.common.graphql.generated.DeliveryArea
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorker
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatus
import java.math.BigDecimal

/**
 * 领域在线状态 → GraphQL DeliveryWorkerStatus(同名 valueOf 互转)
 */
fun DeliverWorkerModelStatus.toGraphQL(): DeliveryWorkerStatus = DeliveryWorkerStatus.valueOf(name)

/**
 * AdminModel(送水员账号行)→ GraphQL DeliveryWorker(原 DeliveryWorkerMapper ObjectMappie 迁移)。
 *
 * 送水员档案并入 admins 单表后(PRD/07-user.md §F-USR-021 目标架构),DeliveryWorker
 * 为兼容投影类型:id 即账号 id;wechatOpenId 不再落库,投影为空串。
 */
fun AdminModel.toDeliveryWorker(): DeliveryWorker =
  DeliveryWorker(
    id = id ?: 0L,
    userId = userId ?: 0L,
    wechatOpenId = "",
    name = realName ?: username,
    phone = phone ?: "",
    role = role.name,
    earningCents = earningCents,
    onlineStatus = onlineStatus.toGraphQL(),
    // 账号状态(枚举需显式转换;F-USR-021)
    accountStatus = AdminAccountStatus.valueOf(accountStatus.name),
    // 任务数由调用方联查填充,直映恒为 0
    currentTaskCount = 0,
    avatarUrl = avatarUrl,
    averageRating = averageRating?.let { BigDecimal.valueOf(it) },
    completedOrders = completedOrders,
    coordinates = coordinates,
    createdAt = createdAt,
    currentLocation = currentLocation,
    isAvailable = isAvailable,
    rating = rating?.let { BigDecimal.valueOf(it) },
    totalOrders = totalOrders,
    updatedAt = updatedAt,
  )

/**
 * DeliveryAreaModel → GraphQL DeliveryArea(原 DeliveryAreaMapper ObjectMappie 迁移)。
 */
fun DeliveryAreaModel.toGraphQL(): DeliveryArea =
  DeliveryArea(
    id = id ?: 0L,
    name = name,
    province = province,
    city = city,
    district = district,
    enabled = enabled,
  )
