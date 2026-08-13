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

import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 配送费规则配置实体(单行配置,固定 id = 1)
 * 与 PRD 12-operations-config.md §F-OPS-005 / 04-order.md §R-MON-02 保持一致(楼层口径):
 * 免费楼层 + 超出一次性加价;未配置时按默认规则(免费4楼/加价2元)。
 * 金额以分存储与服务端计算(R-MON-01)。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "delivery_fee_rules")
open class DeliveryFeeRuleModel(
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "free_floor_limit", nullable = false)
  var freeFloorLimit: Int = DEFAULT_FREE_FLOOR_LIMIT,
  @Column(name = "surcharge_cents", nullable = false)
  var surchargeCents: Long = DEFAULT_SURCHARGE_CENTS,
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  companion object {
    /** 默认免费楼层:4楼及以下免运费 */
    const val DEFAULT_FREE_FLOOR_LIMIT = 4

    /** 默认超出加价:2元 */
    const val DEFAULT_SURCHARGE_CENTS = 200L
  }
}
