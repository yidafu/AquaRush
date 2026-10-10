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

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 店铺信息配置实体(单行配置,固定 id = 1)
 * 与 PRD 12-operations-config.md §F-OPS-004 保持一致:
 * 店铺名称、LOGO、客服电话、营业时间、店铺简介、关于我们;未配置时用户端按默认值兜底。
 */
open class StoreProfileModel(
  var id: Long? = null,
  var shopName: String = "",
  var logoUrl: String? = null,
  var serviceHotline: String = "",
  var businessHoursStart: LocalTime? = null,
  var businessHoursEnd: LocalTime? = null,
  var shopIntro: String? = null,
  var aboutUs: String? = null,
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) : SoftDeletable
