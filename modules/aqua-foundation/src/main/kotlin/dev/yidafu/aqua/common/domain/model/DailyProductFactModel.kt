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
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 商品日事实(第 3 层历史趋势指标中间表,夜间快照任务写入)
 * 原子粒度:stat_date × status × product_id
 * - quantity:商品件数合计
 * - revenue_cents:商品营收合计(order_items.total_price_cents 汇总,单价×数量)
 * - order_count:包含该商品的去重订单数
 * 商品名易变,不在事实表落库;查询时 join 商品表取名。已完成口径(仅 COMPLETED)在查询时组合。
 */
@Entity
@Table(
  name = "daily_product_facts",
  uniqueConstraints = [UniqueConstraint(columnNames = ["stat_date", "status", "product_id"])],
)
data class DailyProductFactModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "stat_date", nullable = false)
  var statDate: LocalDate = LocalDate.now(),
  @Column(name = "status", nullable = false)
  var status: String = "",
  @Column(name = "product_id", nullable = false)
  var productId: Long = 0,
  @Column(name = "quantity", nullable = false)
  var quantity: Long = 0,
  @Column(name = "revenue_cents", nullable = false)
  var revenueCents: Long = 0,
  @Column(name = "order_count", nullable = false)
  var orderCount: Long = 0,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
