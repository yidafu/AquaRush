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
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 满减/折扣活动(PRD 10-marketing.md §F-MKT-004/R-MKT-04):
 * 订单级自动营销活动,满足门槛即自动减免,无需领券;多档阶梯(档位递增且互斥);
 * 适用范围全场/指定商品/指定分组;时间窗到期自动失效;同一订单命中多个活动取优惠最大者不叠加。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "promotion_activities")
open class PromotionActivityModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "name", nullable = false, length = 100)
  var name: String = "",
  @Column(name = "activity_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var activityType: PromotionActivityType = PromotionActivityType.FULL_REDUCE,
  @Column(name = "scope", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var scope: PromotionScope = PromotionScope.ALL,
  @Column(name = "product_id")
  var productId: Long? = null,
  @Column(name = "customer_group_id")
  var customerGroupId: Long? = null,
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
