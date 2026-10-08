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
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import jakarta.persistence.*
import org.hibernate.annotations.SoftDelete
import java.time.LocalDateTime

/**
 * 首页轮播图(运营位)实体
 * 与 PRD 12-operations-config.md §F-OPS-001 / 13-misc.md §核心实体 保持一致:
 * 图片、标题/副标题、跳转链接、排序、时间窗、状态(启用/停用);时间窗为空=长期。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(
  name = "banners",
  indexes = [Index(name = "idx_banners_sort_order", columnList = "sort_order")],
)
open class BannerModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "image_url", nullable = false, length = 1000)
  var imageUrl: String = "",
  @Column(name = "title", nullable = false, length = 20)
  var title: String = "",
  @Column(name = "subtitle", length = 100)
  var subtitle: String? = null,
  @Column(name = "link_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  var linkType: BannerLinkType = BannerLinkType.CUSTOM_PAGE,
  @Column(name = "link_target", nullable = false, length = 1000)
  var linkTarget: String = "",
  @Column(name = "sort_order", nullable = false)
  var sortOrder: Int = 0,
  @Column(name = "enabled", nullable = false)
  var enabled: Boolean = false,
  @Column(name = "start_at")
  var startAt: LocalDateTime? = null,
  @Column(name = "end_at")
  var endAt: LocalDateTime? = null,
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
