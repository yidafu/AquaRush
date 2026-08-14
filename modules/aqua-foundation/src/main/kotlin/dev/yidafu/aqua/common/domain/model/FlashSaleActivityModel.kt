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
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 限时特价活动(PRD 10-marketing.md §F-MKT-005/R-MKT-05):
 * 商品级限时活动价,处于活动期的商品按活动价参与成交单价解析(R-MKT-02③);
 * 时间窗到期自动失效(查询按时间窗过滤,无需定时任务);已生成订单不受影响。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "flash_sale_activities",
  indexes = [
    Index(name = "idx_flash_sale_product_time", columnList = "product_id,valid_from,valid_to"),
  ],
)
open class FlashSaleActivityModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "name", nullable = false, length = 100)
  var name: String = "",
  @Column(name = "product_id", nullable = false)
  var productId: Long = 0,
  @Column(name = "sale_price_cents", nullable = false)
  var salePriceCents: Long = 0,
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
