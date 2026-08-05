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

import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime
import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "admins")
data class AdminModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "username", unique = true, nullable = false, length = 50)
  var username: String,
  @Column(name = "password_hash", nullable = false)
  var passwordHash: String,
  @Column(name = "real_name", length = 100)
  var realName: String? = null,
  @Column(name = "phone", length = 20)
  var phone: String? = null,
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  var role: AdminRoleModel = AdminRoleModel.ADMIN,
  @Column(name = "user_id")
  var userId: Long? = null,
  @Column(name = "delivery_worker_id")
  var deliveryWorkerId: Long? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "last_login_at")
  var lastLoginAt: LocalDateTime? = null,
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
