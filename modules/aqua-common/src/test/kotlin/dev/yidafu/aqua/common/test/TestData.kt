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

package dev.yidafu.aqua.common.test

import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.enums.AdminRoleModel
import dev.yidafu.aqua.common.enums.UserStatus
import java.time.LocalDateTime

/**
 * 测试数据辅助类，提供常用的测试数据生成方法
 */
object TestData {
  fun sampleUser(
    id: Long = 1L,
    username: String = "testuser",
    phone: String = "13800138000",
    status: UserStatus = UserStatus.ACTIVE,
  ) = UserModel(
    id = id,
    wechatOpenId = "test_openid_$id",
    nickname = username,
    phone = phone,
    status = status,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
  )

  fun sampleAdmin(
    id: Long = 1L,
    username: String = "admin",
    phone: String = "13900139000",
    role: AdminRoleModel = AdminRoleModel.ADMIN,
  ) = AdminModel(
    id = id,
    username = username,
    passwordHash = "hashed_password",
    phone = phone,
    role = role,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
  )

  fun sampleAddress(
    id: Long = 1L,
    userId: Long = 1L,
    detail: String = "Test Address",
    isDefault: Boolean = true,
  ) = AddressModel(
    id = id,
    userId = userId,
    receiverName = "测试用户",
    phone = "13800138000",
    province = "广东省",
    city = "深圳市",
    district = "南山区",
    detailAddress = detail,
    isDefault = isDefault,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
  )

  fun sampleRegion(
    id: Long = 1L,
    name: String = "广东省",
    code: String = "440000",
    parentCode: String? = null,
    level: Int = 1,
  ) = RegionModel(
    id = id,
    name = name,
    code = code,
    parentCode = parentCode,
    level = level,
    createdAt = LocalDateTime.now(),
    updatedAt = LocalDateTime.now(),
  )

  val sampleProvince = sampleRegion(1L, "广东省", "440000", null, 1)
  val sampleCity = sampleRegion(2L, "深圳市", "440300", "440000", 2)
  val sampleDistrict = sampleRegion(3L, "南山区", "440305", "440300", 3)

  val anotherCity = sampleRegion(4L, "广州市", "440100", "440000", 2)
  val anotherDistrict = sampleRegion(5L, "天河区", "440106", "440100", 3)
}