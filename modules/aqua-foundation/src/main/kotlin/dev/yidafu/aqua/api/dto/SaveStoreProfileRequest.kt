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

package dev.yidafu.aqua.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalTime

/**
 * 保存店铺信息请求(单行配置,整体覆盖保存)
 * 与 PRD 12-operations-config.md §F-OPS-004 保持一致:
 * 店铺名称≤20字、客服电话格式校验、店铺简介≤100字、关于我们≤2000字。
 */
data class SaveStoreProfileRequest(
  @field:NotBlank(message = "店铺名称不能为空")
  @field:Size(max = 20, message = "店铺名称不能超过20个字")
  val shopName: String,
  @field:Size(max = 1000, message = "LOGO 地址长度不能超过1000个字符")
  val logoUrl: String? = null,
  @field:NotBlank(message = "客服电话不能为空")
  @field:Pattern(
    regexp = "^1[3-9]\\d{9}$|^0\\d{2,3}-?\\d{7,8}$|^400-?\\d{3}-?\\d{4}$",
    message = "客服电话格式不正确",
  )
  val serviceHotline: String,
  val businessHoursStart: LocalTime? = null,
  val businessHoursEnd: LocalTime? = null,
  @field:Size(max = 100, message = "店铺简介不能超过100个字")
  val shopIntro: String? = null,
  @field:Size(max = 2000, message = "关于我们内容不能超过2000个字")
  val aboutUs: String? = null,
)
