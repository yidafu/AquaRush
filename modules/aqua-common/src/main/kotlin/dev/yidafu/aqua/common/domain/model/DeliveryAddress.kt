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
import java.time.LocalDateTime

@Entity
@Table(name = "delivery_addresses")
data class DeliveryAddress(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long,
  @Column(name = "receiver_name", nullable = false)
  var receiverName: String,
  @Column(name = "receiver_phone", nullable = false)
  var receiverPhone: String,
  @Column(name = "province", nullable = false)
  var province: String,
  @Column(name = "city", nullable = false)
  var city: String,
  @Column(name = "district", nullable = false)
  var district: String,
  @Column(name = "detail_address", nullable = false)
  var detailAddress: String,
  @Column(name = "longitude")
  var longitude: Double? = null,
  @Column(name = "latitude")
  var latitude: Double? = null,
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
