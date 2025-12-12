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
  @field:JsonProperty("receiverName")
  @field:NotBlank(message = "收货人姓名不能为空")
  @field:Size(min = 2, max = 20, message = "收货人姓名长度应在2-20个字符之间")
  val receiverName: String,
  @field:JsonProperty("phone")
  @field:NotBlank(message = "收货人手机号不能为空")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String,
  @field:JsonProperty("province")
  @field:NotBlank(message = "省份不能为空")
  val province: String,
  @field:JsonProperty("provinceCode")
  @field:Size(min = 1, max = 20, message = "省份代码长度应在1-20个字符之间")
  val provinceCode: String? = null,
  @field:JsonProperty("city")
  @field:NotBlank(message = "城市不能为空")
  val city: String,
  @field:JsonProperty("cityCode")
  @field:Size(min = 1, max = 20, message = "城市代码长度应在1-20个字符之间")
  val cityCode: String? = null,
  @field:JsonProperty("district")
  @field:NotBlank(message = "区县不能为空")
  val district: String,
  @field:JsonProperty("districtCode")
  @field:Size(min = 1, max = 20, message = "区县代码长度应在1-20个字符之间")
  val districtCode: String? = null,
  @field:JsonProperty("detailAddress")
  @field:NotBlank(message = "详细地址不能为空")
  @field:Size(min = 5, max = 200, message = "详细地址长度应在5-200个字符之间")
  val detailAddress: String,
  @field:JsonProperty("longitude")
  @field:DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
  @field:DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
  val longitude: Double? = null,
  @field:JsonProperty("latitude")
  @field:DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
  @field:DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
  val latitude: Double? = null,
  @field:JsonProperty("isDefault")
  val isDefault: Boolean = false,
)
