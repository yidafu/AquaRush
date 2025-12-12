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
import java.time.LocalDateTime

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
  @field:JsonProperty("provinceCode")
  val provinceCode: String? = null,
  @field:JsonProperty("city")
  @field:NotBlank(message = "城市不能为空")
  val city: String,
  @field:JsonProperty("cityCode")
  val cityCode: String? = null,
  @field:JsonProperty("district")
  @field:NotBlank(message = "区县不能为空")
  val district: String,
  @field:JsonProperty("districtCode")
  val districtCode: String? = null,
  @field:JsonProperty("detailAddress")
  @field:NotBlank(message = "详细地址不能为空")
  val detailAddress: String,
  @field:JsonProperty("longitude")
  val longitude: Double? = null,
  @field:JsonProperty("latitude")
  val latitude: Double? = null,
  @field:JsonProperty("isDefault")
  @field:NotNull(message = "是否默认地址不能为空")
  val isDefault: Boolean = false,
  @field:JsonProperty("createdAt")
  val createdAt: LocalDateTime? = null,
  @field:JsonProperty("updatedAt")
  val updatedAt: LocalDateTime? = null,
)
