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
import java.time.LocalDateTime

/**
 * 订单赠品行(PRD 10-marketing.md §R-MKT-09):
 * 买赠/满赠活动达标后随订单落库的赠品快照(商品名随单快照,活动配置变更不影响已生成订单);
 * 赠品为附加 0 价行,不参与现金计价与叠加;随订单生命周期,不做软删。
 */
@Entity
@Table(
  name = "order_gifts",
  indexes = [
    Index(name = "idx_order_gifts_order", columnList = "order_id"),
  ],
)
open class OrderGiftModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "order_id", nullable = false)
  var orderId: Long = 0,
  @Column(name = "gift_activity_id")
  var giftActivityId: Long? = null,
  @Column(name = "gift_activity_name", length = 100)
  var giftActivityName: String? = null,
  @Column(name = "product_id", nullable = false)
  var productId: Long = 0,
  @Column(name = "product_name", nullable = false, length = 100)
  var productName: String = "",
  @Column(name = "quantity", nullable = false)
  var quantity: Int = 0,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
