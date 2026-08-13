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
 * 系统参数类型
 * 与 PRD 11-system-settings.md §F-SYS-001 保持一致(数值/布尔/文本/枚举),
 * 值按声明类型校验(数值区间/布尔/枚举/长度,R-SYS-01)。
 */
enum class SystemSettingType(
  val label: String,
) {
  NUMBER("数值"),
  BOOLEAN("布尔"),
  STRING("文本"),
  ENUM("枚举"),
}
