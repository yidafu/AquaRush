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

import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import java.time.LocalDateTime

/**
 * 首页轮播图(运营位)实体
 * 与 PRD 12-operations-config.md §F-OPS-001 / 13-misc.md §核心实体 保持一致:
 * 图片、标题/副标题、跳转链接、排序、时间窗、状态(启用/停用);时间窗为空=长期。
 */
open class BannerModel(
  var id: Long? = null,
  var imageUrl: String = "",
  var title: String = "",
  var subtitle: String? = null,
  var linkType: BannerLinkType = BannerLinkType.CUSTOM_PAGE,
  var linkTarget: String = "",
  var sortOrder: Int = 0,
  var enabled: Boolean = false,
  var startAt: LocalDateTime? = null,
  var endAt: LocalDateTime? = null,
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable
