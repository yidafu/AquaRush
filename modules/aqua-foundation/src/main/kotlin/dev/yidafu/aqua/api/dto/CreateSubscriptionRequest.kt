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

import com.fasterxml.jackson.annotation.JsonProperty
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 创建周期订购计划请求(F-ORD-026/R-ORD-13,后台入口):
 * 管理员为指定用户绑定收货地址创建计划;到期由调度自动生成代客下单订单并提醒送水工。
 * 日期一律 `yyyy-MM-dd` 字符串(跨层传输不引入新 GraphQL 日期标量),服务层解析校验。
 */
data class CreateSubscriptionRequest(
  @field:JsonProperty("userId")
  @field:NotNull(message = "用户ID不能为空")
  val userId: Long,
  @field:JsonProperty("addressId")
  @field:NotNull(message = "配送地址ID不能为空")
  val addressId: Long,
  // 绑定送水工(可空):设置后到期定向提醒该送水工
  @field:JsonProperty("workerId")
  val workerId: Long? = null,
  @field:JsonProperty("items")
  @field:Valid
  @field:NotEmpty(message = "计划商品明细不能为空")
  @field:Size(min = 1, max = 20, message = "计划商品明细数量需在1-20项之间")
  val items: List<SubscriptionItemRequest>,
  @field:JsonProperty("cycleType")
  @field:NotNull(message = "配送周期不能为空")
  val cycleType: SubscriptionCycleType,
  // WEEKLY/BIWEEKLY 必填(ISO 1=周一..7=周日,去重);MONTHLY 忽略
  @field:JsonProperty("weekdays")
  val weekdays: List<Int>? = null,
  // MONTHLY 必填(1~31);WEEKLY/BIWEEKLY 忽略
  @field:JsonProperty("monthDay")
  val monthDay: Int? = null,
  @field:JsonProperty("timeSlot")
  @field:NotNull(message = "配送时段不能为空")
  val timeSlot: DeliveryTimeSlot,
  // 生效日期 yyyy-MM-dd
  @field:JsonProperty("startDate")
  @field:NotNull(message = "生效日期不能为空")
  val startDate: String,
  // 终止日期 yyyy-MM-dd(可空);与 maxPeriods 至少其一可空=长期有效
  @field:JsonProperty("endDate")
  val endDate: String? = null,
  // 期次上限(可空,≥1)
  @field:JsonProperty("maxPeriods")
  val maxPeriods: Int? = null,
  @field:JsonProperty("remark")
  @field:Size(max = 500, message = "备注长度不能超过500个字符")
  val remark: String? = null,
)

/** 周期订购计划商品明细项 */
data class SubscriptionItemRequest(
  @field:JsonProperty("productId")
  @field:NotNull(message = "商品ID不能为空")
  val productId: Long,
  @field:JsonProperty("quantity")
  @field:NotNull(message = "数量不能为空")
  @field:Min(value = 1, message = "数量必须大于0")
  @field:Max(value = 999, message = "数量不能超过999")
  val quantity: Int,
)

/**
 * 周期订购计划列表查询(后台列表页,分页 + 筛选;禁止全表,AGENTS 规则 10):
 * keyword 匹配用户昵称/手机号;nextRunFrom/nextRunTo 为 yyyy-MM-dd 下次配送时间区间。
 */
data class SubscriptionListQuery(
  val keyword: String? = null,
  val status: String? = null,
  val nextRunFrom: String? = null,
  val nextRunTo: String? = null,
  val page: Int = 0,
  val size: Int = 20,
)
