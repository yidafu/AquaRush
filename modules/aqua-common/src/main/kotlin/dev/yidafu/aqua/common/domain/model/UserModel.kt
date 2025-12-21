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

import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.utils.MoneyUtils
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "users")
open class  UserModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "wechat_openid", unique = true, nullable = false)
  val wechatOpenId: String = "",

  @Column(name = "nickname")
  var nickname: String? = null,

  @Column(name = "phone")
  var phone: String? = null,

  @Column(name = "avatar_url")
  var avatarUrl: String? = null,

  @Column(name = "email", nullable = false)
  var email: String = "",

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  val status: UserStatus = UserStatus.INACTIVE,

  @Column(name = "role", nullable = false)
  @Enumerated(EnumType.STRING)
  val role: UserRole = UserRole.NONE,

  @Column(name = "balance_cents", nullable = false)
  val balanceCents: Long = 0L,

  @Column(name = "total_spent_cents", nullable = false)
  val totalSpentCents: Long = 0L,

  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "last_login_at", nullable = false)
  var lastLoginAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,

  @Column(name = "deleted_by")
  override var deletedBy: Long? = null
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  // Backward compatibility property
  val balance: BigDecimal
  get() = MoneyUtils.fromCents(balanceCents)

  // Backward compatibility property
  val totalSpent: BigDecimal
    get() = MoneyUtils.fromCents(totalSpentCents)
}
