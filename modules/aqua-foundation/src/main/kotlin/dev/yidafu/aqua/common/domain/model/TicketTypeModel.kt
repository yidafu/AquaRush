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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 桶水票票种(PRD 10-marketing.md §F-MKT-007/R-MKT-08):
 * 指定商品票(仅抵指定商品)/通兑票(抵限定规格下全场商品,跨规格不通用);
 * 套餐:张数+购票价(分)+赠送张数(买N送M);有效期固定区间或购票后N天;限购(0=不限)。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "ticket_types")
open class TicketTypeModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "name", nullable = false, length = 100)
  var name: String = "",
  @Column(name = "ticket_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var ticketType: TicketTypeKind = TicketTypeKind.UNIVERSAL,
  @Column(name = "product_id")
  var productId: Long? = null,
  // 通兑票限定规格(与商品 specification 字符串匹配,如 "19L")
  @Column(name = "specification", length = 100)
  var specification: String? = null,
  @Column(name = "package_count", nullable = false)
  var packageCount: Int = 0,
  @Column(name = "package_price_cents", nullable = false)
  var packagePriceCents: Long = 0,
  @Column(name = "gift_count", nullable = false)
  var giftCount: Int = 0,
  @Column(name = "validity_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var validityType: ValidityType = ValidityType.FIXED_RANGE,
  @Column(name = "valid_from")
  var validFrom: LocalDateTime? = null,
  @Column(name = "valid_to")
  var validTo: LocalDateTime? = null,
  @Column(name = "valid_days")
  var validDays: Int? = null,
  @Column(name = "per_user_limit", nullable = false)
  var perUserLimit: Int = 0,
  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var status: TicketTypeStatus = TicketTypeStatus.ACTIVE,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
