/*
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "delivery_workers")
data class DeliveryWorker(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long,
  @Column(name = "wechat_openid", unique = true, nullable = false)
  var wechatOpenId: String,
  @Column(name = "name", nullable = false)
  var name: String,
  @Column(name = "phone", unique = true, nullable = false)
  var phone: String,
  @Column(name = "avatar_url")
  var avatarUrl: String?,
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  var status: WorkerStatus = WorkerStatus.OFFLINE,
  @Column(name = "coordinates", columnDefinition = "jsonb")
  var coordinates: String? = null,
  @Column(name = "current_location", columnDefinition = "jsonb")
  var currentLocation: String? = null, // 存储 JSON 格式的坐标
  @Column(name = "rating", precision = 2, scale = 1)
  var rating: BigDecimal? = null,
  @Column(name = "total_orders", nullable = false)
  var totalOrders: Int = 0,
  @Column(name = "completed_orders", nullable = false)
  var completedOrders: Int = 0,
  @Column(name = "average_rating", precision = 2, scale = 1)
  var averageRating: BigDecimal? = null,
  @Column(name = "earning", precision = 10, scale = 2)
  var earning: BigDecimal? = null,
  @Column(name = "is_available", nullable = false)
  var isAvailable: Boolean = true,
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

enum class WorkerStatus {
  ONLINE, // 上线
  OFFLINE, // 下线
}
