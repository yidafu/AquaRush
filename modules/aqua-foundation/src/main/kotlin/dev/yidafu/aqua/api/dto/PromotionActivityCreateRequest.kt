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

import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 新建满减/折扣活动请求(PRD 10-marketing.md §F-MKT-004/R-MKT-04):
 * 多档位(递增且互斥,服务端校验);适用范围全场/指定商品/指定分组;时间窗必填。
 */
data class PromotionActivityCreateRequest(
  @field:NotBlank(message = "活动名称不能为空")
  @field:Size(max = 100, message = "活动名称不能超过100个字")
  val name: String,
  @field:NotNull(message = "活动类型不能为空")
  val activityType: PromotionActivityType,
  @field:NotNull(message = "适用范围不能为空")
  val scope: PromotionScope,
  val productId: Long? = null,
  val customerGroupId: Long? = null,
  @field:NotNull(message = "开始时间不能为空")
  val validFrom: LocalDateTime,
  @field:NotNull(message = "结束时间不能为空")
  val validTo: LocalDateTime,
  @field:NotEmpty(message = "至少需要一个档位")
  val tiers: List<@Valid PromotionTierItem>,
)
