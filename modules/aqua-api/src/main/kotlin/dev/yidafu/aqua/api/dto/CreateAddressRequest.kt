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

/**
 * 创建地址请求DTO
 */
data class CreateAddressRequest(
  @field:JsonProperty("province")
  @field:NotBlank(message = "省份不能为空")
  val province: String,
  @field:JsonProperty("city")
  @field:NotBlank(message = "城市不能为空")
  val city: String,
  @field:JsonProperty("district")
  @field:NotBlank(message = "区域不能为空")
  val district: String,
  @field:JsonProperty("street")
  @field:NotBlank(message = "街道地址不能为空")
  val street: String,
  @field:JsonProperty("detailedAddress")
  @field:NotBlank(message = "详细地址不能为空")
  val detailedAddress: String,
  @field:JsonProperty("postalCode")
  @field:Size(max = 10, message = "邮政编码长度不能超过10个字符")
  val postalCode: String? = null,
  @field:JsonProperty("phone")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String,
  @field:JsonProperty("isDefault")
  val isDefault: Boolean = false,
)
