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

package dev.yidafu.aqua.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 用户数据传输对象
 */
data class UserDTO(
  @field:JsonProperty("id")
  @field:NotNull(message = "用户ID不能为空")
  val id: Long?,
  @field:JsonProperty("wechatOpenId")
  @field:NotNull(message = "微信OpenID不能为空")
  @field:NotBlank(message = "微信OpenID不能为空")
  val wechatOpenId: String?,
  @field:JsonProperty("nickname")
  @field:Size(max = 50, message = "昵称长度不能超过50个字符")
  val nickname: String?,
  @field:JsonProperty("phone")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String?,
  @field:JsonProperty("email")
  @field:NotBlank(message = "邮箱不能为空")
  @field:Email(message = "邮箱格式不正确")
  val email: String,
  @field:JsonProperty("avatar")
  val avatarUrl: String?,
  @field:JsonProperty("status")
  val status: UserStatus,
  @field:JsonProperty("role")
  val role: UserRole?,
  @field:JsonProperty("balance")
  val balance: BigDecimal,
  @field:JsonProperty("totalSpent")
  val totalSpent: BigDecimal,
  @field:JsonProperty("addresses")
  val addresses: List<AddressDTO>,
  @field:JsonProperty("createdAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val createdAt: LocalDateTime,
  @field:JsonProperty("updatedAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val updatedAt: LocalDateTime,
  @field:JsonProperty("lastLoginAt")
  @field:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val lastLoginAt: LocalDateTime,
  @field:JsonProperty("notificationSettings")
  val notificationSettings: NotificationSettingsDTO,
)

/**
 * 用户状态枚举
 */
enum class UserStatus {
  ACTIVE,
  INACTIVE,
  SUSPENDED,
  DELETED,
}

/**
 * 用户角色枚举
 */
enum class UserRole {
  USER,
  WORKER,
  ADMIN,
  NONE,
}

/**
 * 通知设置数据传输对象
 */
data class NotificationSettingsDTO(
  val orderUpdates: Boolean = true,
  val paymentNotifications: Boolean = true,
  val deliveryNotifications: Boolean = true,
  val promotionalNotifications: Boolean = false,
)
