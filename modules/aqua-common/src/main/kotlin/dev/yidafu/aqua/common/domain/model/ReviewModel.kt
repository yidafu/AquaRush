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
import org.hibernate.annotations.Where
import java.time.LocalDateTime

import org.hibernate.annotations.SoftDelete

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "reviews",
  indexes = [
    Index(name = "idx_review_order_id", columnList = "order_id"),
    Index(name = "idx_review_user_id", columnList = "user_id"),
    Index(name = "idx_review_delivery_worker_id", columnList = "delivery_worker_id"),
    Index(name = "idx_review_created_at", columnList = "created_at"),
  ],
)
open class  ReviewModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "order_id", nullable = false, unique = true)
  val orderId: Long,
  @Column(name = "user_id", nullable = false)
  val userId: Long,
  @Column(name = "delivery_worker_id", nullable = false)
  val deliveryWorkerId: Long,
  @Column(name = "rating", nullable = false)
  val rating: Int, // 1-5 stars
  @Column(name = "comment", columnDefinition = "TEXT")
  val comment: String? = null,
  @Column(name = "is_anonymous", nullable = false)
  val isAnonymous: Boolean = false,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
@Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,

  @Column(name = "deleted_by")
  override var deletedBy: Long? = null
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  init {
    require(rating in 1..5) { "Rating must be between 1 and 5" }
    require(comment?.length ?: 0 <= 500) { "Comment cannot exceed 500 characters" }
  }
}
