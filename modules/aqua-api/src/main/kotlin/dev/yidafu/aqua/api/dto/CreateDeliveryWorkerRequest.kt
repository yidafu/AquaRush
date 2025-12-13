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

import dev.yidafu.aqua.common.domain.model.DeliverWorkerStatus
import jakarta.validation.constraints.*

/**
 * 创建配送员请求数据传输对象
 */
data class CreateDeliveryWorkerRequest(
  @field:NotBlank(message = "姓名不能为空")
  @field:Size(max = 50, message = "姓名长度不能超过50个字符")
  val name: String,
  @field:NotBlank(message = "手机号不能为空")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String,
  @field:NotNull(message = "配送区域ID不能为空")
  val deliveryAreaId: Long,
  @field:Size(max = 500, message = "备注长度不能超过500个字符")
  val notes: String? = null,
  @field:NotNull(message = "状态不能为空")
  val status: DeliverWorkerStatus = DeliverWorkerStatus.OFFLINE,
)
