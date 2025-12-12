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
@Table(name = "regions")
data class RegionModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = -1L,

  @Column(name = "name", nullable = false)
  val name: String,

  @Column(name = "code", unique = true, nullable = false)
  val code: String,

  @Column(name = "parent_code")
  val parentCode: String? = null,

  @Column(name = "level", nullable = false)
  val level: Int,

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
