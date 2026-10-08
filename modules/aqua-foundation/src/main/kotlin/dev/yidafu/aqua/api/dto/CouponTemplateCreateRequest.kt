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

import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 新建优惠券模板请求(PRD 10-marketing.md §F-MKT-003/R-MKT-03):
 * 现金券用 discountAmountCents,折扣券用 discountRatePermille(千分比,8500=8.5折);门槛空=无门槛;金额以分计。
 */
data class CouponTemplateCreateRequest(
  @field:NotBlank(message = "券名称不能为空")
  @field:Size(max = 100, message = "券名称不能超过100个字")
  val name: String,
  @field:NotNull(message = "券类型不能为空")
  val couponType: CouponType,
  @field:Min(value = 0, message = "门槛金额无效")
  val thresholdCents: Long? = null,
  @field:Min(value = 1, message = "券面额无效")
  @field:Max(value = 9999900, message = "券面额无效(0.01-99999元)")
  val discountAmountCents: Long? = null,
  @field:Min(value = 100, message = "折扣率超出范围(0.1-9.9折)")
  @field:Max(value = 9990, message = "折扣率超出范围(0.1-9.9折)")
  val discountRatePermille: Long? = null,
  @field:NotNull(message = "适用范围不能为空")
  val scope: CouponScope,
  val productId: Long? = null,
  @field:NotNull(message = "有效期类型不能为空")
  val validityType: ValidityType,
  val validFrom: LocalDateTime? = null,
  val validTo: LocalDateTime? = null,
  @field:Min(value = 1, message = "有效天数无效")
  @field:Max(value = 3650, message = "有效天数不能超过3650天")
  val validDays: Int? = null,
  @field:NotNull(message = "发放方式不能为空")
  val issuanceType: CouponIssuanceType,
  @field:NotNull(message = "限领数不能为空")
  @field:Min(value = 1, message = "限领数不能小于1")
  @field:Max(value = 100, message = "限领数不能超过100")
  val perUserLimit: Int,
)
