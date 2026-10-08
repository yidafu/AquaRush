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
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 买赠/满赠活动(PRD 10-marketing.md §F-MKT-008/R-MKT-09):
 * 订单达标自动赠送指定商品(赠品),赠品为附加 0 价行随单配送,不参与现金计价与叠加;
 * 满赠按现金部分商品小计判定,买赠按指定商品现金购买数量判定;v1 单档单赠品。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "gift_activities",
  indexes = [
    Index(name = "idx_gift_activities_active", columnList = "status,valid_from,valid_to"),
  ],
)
open class GiftActivityModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "name", nullable = false, length = 100)
  var name: String = "",
  @Column(name = "gift_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var giftType: GiftActivityType = GiftActivityType.FULL_GIFT,
  @Column(name = "threshold_cents")
  var thresholdCents: Long? = null,
  @Column(name = "threshold_quantity")
  var thresholdQuantity: Int? = null,
  @Column(name = "scope", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var scope: PromotionScope = PromotionScope.ALL,
  @Column(name = "product_id")
  var productId: Long? = null,
  @Column(name = "customer_group_id")
  var customerGroupId: Long? = null,
  @Column(name = "gift_product_id", nullable = false)
  var giftProductId: Long = 0,
  @Column(name = "gift_quantity", nullable = false)
  var giftQuantity: Int = 0,
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
