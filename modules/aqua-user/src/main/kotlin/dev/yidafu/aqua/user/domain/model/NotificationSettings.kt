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

package dev.yidafu.aqua.user.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notification_settings")
data class NotificationSettings(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "user_id", unique = true, nullable = false)
  val userId: Long,

  @Column(name = "order_updates", nullable = false)
  var orderUpdates: Boolean = true,

  @Column(name = "payment_notifications", nullable = false)
  var paymentNotifications: Boolean = true,

  @Column(name = "delivery_notifications", nullable = false)
  var deliveryNotifications: Boolean = true,

  @Column(name = "promotional_notifications", nullable = false)
  var promotionalNotifications: Boolean = false,

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now()
) {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
