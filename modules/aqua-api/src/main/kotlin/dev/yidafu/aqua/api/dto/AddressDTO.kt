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

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*
import java.util.*

/**
 * 地址数据传输对象
 */
data class AddressDTO(
  @field:JsonProperty("id")
  @field:NotNull(message = "地址ID不能为空")
  val id: Long?,
  @field:JsonProperty("userId")
  @field:NotNull(message = "用户ID不能为空")
  val userId: Long,
  @field:JsonProperty("receiverName")
  @field:NotBlank(message = "收货人姓名不能为空")
  val receiverName: String,
  @field:JsonProperty("phone")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String,
  @field:JsonProperty("province")
  @field:NotBlank(message = "省份不能为空")
  val province: String,
  @field:JsonProperty("city")
  @field:NotBlank(message = "城市不能为空")
  val city: String,
  @field:JsonProperty("district")
  @field:NotBlank(message = "区域不能为空")
  val district: String,
  @field:JsonProperty("detailAddress")
  @field:NotBlank(message = "详细地址不能为空")
  val detailAddress: String,
  @field:JsonProperty("postalCode")
  @field:Size(max = 10, message = "邮政编码长度不能超过10个字符")
  val postalCode: String? = null,
  @field:JsonProperty("coordinates")
  @JsonProperty("coordinates")
  val coordinates: Coordinates? = null,
  @field:JsonProperty("isDefault")
  @field:NotNull(message = "是否默认地址不能为空")
  val isDefault: Boolean = false,
)

class Coordinates
