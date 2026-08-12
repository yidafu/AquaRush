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

package dev.yidafu.aqua.common.domain.model.enums

/**
 * 意见反馈处理状态
 * 与 PRD 12-operations-config.md §F-OPS-003 保持一致(待处理 → 处理中 → 已处理,回退为开放问题)。
 */
enum class FeedbackStatus(
  val label: String,
) {
  PENDING("待处理"),
  PROCESSING("处理中"),
  RESOLVED("已处理"),
}
