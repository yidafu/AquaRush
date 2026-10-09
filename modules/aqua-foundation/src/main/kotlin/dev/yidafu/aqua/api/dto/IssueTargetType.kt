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

package dev.yidafu.aqua.api.dto

/**
 * 定向发放目标类型(PRD 10-marketing.md §F-MKT-003):
 * targetType=USER 按 userIds 发放;GROUP 按 customerGroupId 发放给分组全部成员。
 */
enum class IssueTargetType {
  /** 按用户列表发放 */
  USER,

  /** 按客户分组发放(全组成员) */
  GROUP,
}
