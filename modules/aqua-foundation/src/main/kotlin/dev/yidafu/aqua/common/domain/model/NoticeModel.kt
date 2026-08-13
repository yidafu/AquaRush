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
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 平台公告实体
 * 与 PRD 12-operations-config.md §F-OPS-002 / 13-misc.md §核心实体 保持一致:
 * 标题、内容、类型、置顶、时间窗、状态(草稿/已发布/已停用);时间窗为空=长期。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "notices",
  indexes = [Index(name = "idx_notices_pinned_published_at", columnList = "pinned, published_at")],
)
open class NoticeModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "title", nullable = false, length = 50)
  var title: String = "",
  @Column(name = "content", nullable = false, columnDefinition = "text")
  var content: String = "",
  @Column(name = "type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var type: NoticeType = NoticeType.PLATFORM_NOTICE,
  @Column(name = "pinned", nullable = false)
  var pinned: Boolean = false,
  @Column(name = "start_at")
  var startAt: LocalDateTime? = null,
  @Column(name = "end_at")
  var endAt: LocalDateTime? = null,
  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var status: NoticeStatus = NoticeStatus.DRAFT,
  @Column(name = "published_at")
  var publishedAt: LocalDateTime? = null,
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}
