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
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 首单立减/新人专享价活动(PRD 10-marketing.md §F-MKT-009/R-MKT-10):
 * 新用户首次下单自动享受的专属优惠,一单一享(新用户=无已完成订单,首单完成即失效);
 * FIRST_ORDER_REDUCE 进 R-MKT-07 叠加①(与满减互斥取最大),NEWCOMER_PRICE 进 R-MKT-02 单价解析④。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "first_order_activities",
  indexes = [
    Index(name = "idx_first_order_active", columnList = "status,valid_from,valid_to"),
    Index(name = "idx_first_order_product", columnList = "product_id,status"),
  ],
)
open class FirstOrderActivityModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "name", nullable = false, length = 100)
  var name: String = "",
  @Column(name = "activity_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var activityType: FirstOrderActivityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
  // 首单立减:满 X
  @Column(name = "threshold_cents")
  var thresholdCents: Long? = null,
  // 首单立减:减 Y(与折扣率二选一)
  @Column(name = "discount_amount_cents")
  var discountAmountCents: Long? = null,
  // 首单立减:打 N 折(千分比,9000=9折,与减金额二选一)
  @Column(name = "discount_rate_permille")
  var discountRatePermille: Long? = null,
  @Column(name = "scope", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var scope: PromotionScope = PromotionScope.ALL,
  // 新人专享价:指定商品
  @Column(name = "product_id")
  var productId: Long? = null,
  // 新人专享价:专享单价(分)
  @Column(name = "newcomer_price_cents")
  var newcomerPriceCents: Long? = null,
  @Column(name = "valid_from", nullable = false)
  var validFrom: LocalDateTime = LocalDateTime.now(),
  @Column(name = "valid_to", nullable = false)
  var validTo: LocalDateTime = LocalDateTime.now(),
  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var status: PromotionActivityStatus = PromotionActivityStatus.ACTIVE,
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
