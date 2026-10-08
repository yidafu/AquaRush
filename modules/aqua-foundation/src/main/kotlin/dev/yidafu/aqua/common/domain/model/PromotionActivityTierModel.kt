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
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 满减/折扣活动档位(PRD 10-marketing.md §R-MKT-04):
 * 档位金额递增且互斥,活动内取最高已达标档;满减用 discountAmountCents,满折用 discountRatePermille(千分比,9000=9折)。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "promotion_activity_tiers",
  indexes = [
    Index(name = "idx_pat_activity_sort", columnList = "activity_id,sort"),
  ],
)
open class PromotionActivityTierModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "activity_id", nullable = false)
  val activityId: Long = 0,
  @Column(name = "threshold_cents", nullable = false)
  var thresholdCents: Long = 0,
  @Column(name = "discount_amount_cents")
  var discountAmountCents: Long? = null,
  @Column(name = "discount_rate_permille")
  var discountRatePermille: Long? = null,
  @Column(name = "sort", nullable = false)
  var sort: Int = 0,
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
