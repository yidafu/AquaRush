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

package dev.yidafu.aqua.delivery.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "delivery_areas")
data class DeliveryArea(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "name", nullable = false)
  var name: String,
  @Column(name = "province", nullable = false)
  var province: String,
  @Column(name = "city", nullable = false)
  var city: String,
  @Column(name = "district", nullable = false)
  var district: String,
  @Column(name = "enabled", nullable = false)
  var enabled: Boolean = true,
)
