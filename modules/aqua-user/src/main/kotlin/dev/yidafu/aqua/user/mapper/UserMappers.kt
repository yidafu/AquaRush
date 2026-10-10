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

package dev.yidafu.aqua.user.mapper

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.graphql.generated.AddressInput
import dev.yidafu.aqua.common.graphql.generated.AddressVo
import dev.yidafu.aqua.common.graphql.generated.AdminAccountStatus
import dev.yidafu.aqua.common.graphql.generated.AdminRole
import dev.yidafu.aqua.common.graphql.generated.AdminVo
import dev.yidafu.aqua.common.graphql.generated.DeliveryWorkerStatus
import dev.yidafu.aqua.common.graphql.generated.RegionVo
import dev.yidafu.aqua.common.graphql.generated.UpdateAddressInput
import dev.yidafu.aqua.common.graphql.generated.UserVo
import java.math.BigDecimal
import java.time.LocalDateTime

// ============================================================================
// Domain Model → GraphQL 转换
// ============================================================================

/**
 * AdminRole(GraphQL) → AdminRoleModel 枚举同名互转
 */
fun AdminRole.toDomain(): AdminRoleModel = AdminRoleModel.valueOf(name)

/**
 * UserModel → GraphQL UserVo 类型
 * 金额字段两侧均为分制 Long,直接赋值无需转换
 */
fun UserModel.toGraphQL(): UserVo =
  UserVo(
    avatarUrl = avatarUrl,
    balanceCents = balanceCents,
    createdAt = createdAt,
    customerGroupId = customerGroupId,
    // 所属客户分组(F-MKT-001):分组名由 resolver 联查填充(默认 null=未分组)
    customerGroupName = null,
    email = email,
    id = id ?: -1L,
    nickname = nickname,
    phone = phone,
    role = role,
    status = status,
    totalSpentCents = totalSpentCents,
    updatedAt = updatedAt,
    wechatOpenId = wechatOpenId,
  )

/**
 * AdminModel → GraphQL AdminVo 类型
 *
 * 送水员档案并入 admins 单表后,AdminVo 类型同时承载配送业务字段(F-USR-021)
 */
fun AdminModel.toGraphQL(): AdminVo =
  AdminVo(
    // 账号状态(枚举需显式转换;F-USR-020)
    accountStatus = AdminAccountStatus.valueOf(accountStatus.name),
    avatarUrl = avatarUrl,
    averageRating = averageRating?.let { BigDecimal.valueOf(it) },
    completedOrders = completedOrders,
    coordinates = coordinates,
    createdAt = createdAt,
    currentLocation = currentLocation,
    earningCents = earningCents,
    id = id ?: 0L,
    isAvailable = isAvailable,
    lastLoginAt = lastLoginAt,
    // 在线状态(枚举需显式转换)
    onlineStatus = DeliveryWorkerStatus.valueOf(onlineStatus.name),
    phone = phone,
    rating = rating?.let { BigDecimal.valueOf(it) },
    realName = realName,
    role = role.name,
    totalOrders = totalOrders,
    updatedAt = updatedAt,
    username = username,
  )

/**
 * AddressModel → GraphQL AddressVo 类型
 */
fun AddressModel.toGraphQL(): AddressVo =
  AddressVo(
    city = city,
    cityCode = cityCode,
    createdAt = createdAt,
    detailAddress = detailAddress,
    district = district,
    districtCode = districtCode,
    floor = floor,
    id = id ?: -1L,
    isDefault = isDefault,
    // 领域 Double → GraphQL Float(WGS-84 经纬度)
    latitude = latitude?.toFloat(),
    longitude = longitude?.toFloat(),
    phone = phone,
    province = province,
    provinceCode = provinceCode,
    receiverName = receiverName,
    updatedAt = updatedAt,
    userId = userId,
  )

// ============================================================================
// 输入 → Domain Model 转换
// ============================================================================

/**
 * AddressInput → AddressModel(新建地址)
 */
fun AddressInput.toDomain(): AddressModel =
  AddressModel(
    // id 置空交由 @SnowflakeIdGenerator 在 INSERT 时生成;固定 -1 会导致 save() 走 merge 抛 StaleObjectState
    id = null,
    userId = 0L, // Will be set after mapping
    receiverName = receiverName,
    phone = phone,
    province = province,
    provinceCode = provinceCode,
    city = city,
    cityCode = cityCode,
    district = district,
    districtCode = districtCode,
    detailAddress = detailAddress,
    floor = floor,
    // GraphQL Float → 领域 Double(WGS-84 经纬度)
    longitude = longitude?.toDouble(),
    latitude = latitude?.toDouble(),
    isDefault = isDefault,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    deletedAt = null,
    deletedBy = null,
  )

/**
 * UpdateAddressInput → AddressModel(可空字段回落占位值)
 */
fun UpdateAddressInput.toDomain(): AddressModel =
  AddressModel(
    id = -1L,
    userId = 0L, // Will be set after mapping
    receiverName = receiverName ?: "",
    phone = phone ?: "",
    province = province ?: "",
    provinceCode = provinceCode ?: "",
    city = city ?: "",
    cityCode = cityCode ?: "",
    district = district ?: "",
    districtCode = districtCode ?: "",
    detailAddress = detailAddress ?: "",
    floor = floor,
    // GraphQL Float → 领域 Double(WGS-84 经纬度)
    longitude = longitude?.toDouble(),
    latitude = latitude?.toDouble(),
    isDefault = isDefault ?: false,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
    deletedAt = null,
    deletedBy = null,
  )

fun AddressModel.merge(inputInput: UpdateAddressInput) {
  inputInput.receiverName?.let { this.receiverName = it }
  inputInput.phone?.let { this.phone = it }
  inputInput.province?.let { this.province = it }
  inputInput.provinceCode?.let { this.provinceCode = it }
  inputInput.city?.let { this.city = it }
  inputInput.cityCode?.let { this.cityCode = it }
  inputInput.district?.let { this.district = it }
  inputInput.districtCode?.let { this.districtCode = it }
  inputInput.detailAddress?.let { this.detailAddress = it }
  inputInput.floor?.let { this.floor = it }
  inputInput.longitude?.let { this.longitude = it.toDouble() }
  inputInput.latitude?.let { this.latitude = it.toDouble() }
  inputInput.isDefault?.let { this.isDefault = it }
}

/**
 * RegionModel → GraphQL RegionVo 类型
 * id 在未持久化时可能为 null,给 0 兜底;软删除字段不暴露给 GraphQL
 */
fun RegionModel.toGraphQL(): RegionVo =
  RegionVo(
    id = id ?: 0L,
    name = name,
    code = code,
    parentCode = parentCode,
    level = level,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
