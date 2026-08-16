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
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 微信订阅消息授权记录(R-NTF-01)
 *
 * 一次性模板:用户每授权一次可发送一次(remaining_count=1),发送成功后消耗;
 * 达上限/拒绝后不再弹窗授权(微信规范),未授权用户不推送,降级站内查看。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "user_subscription_auths",
  uniqueConstraints = [UniqueConstraint(name = "uk_sub_auth_user_template", columnNames = ["user_id", "template_id"])],
)
open class UserSubscriptionAuthModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long,
  @Column(name = "template_id", nullable = false, length = 100)
  val templateId: String,
  @Column(name = "remaining_count", nullable = false)
  var remainingCount: Int = 1,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
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

  /** 消耗一次授权(一次性模板),返回是否还有剩余 */
  fun consume(): Boolean {
    if (remainingCount <= 0) return false
    remainingCount -= 1
    return true
  }
}
