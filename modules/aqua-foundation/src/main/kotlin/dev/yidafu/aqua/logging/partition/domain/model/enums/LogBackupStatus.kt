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

package dev.yidafu.aqua.logging.partition.domain.model.enums

/**
 * 离线备份记录状态(F-SYS-006)
 */
enum class LogBackupStatus {
  /** 备份进行中 */
  RUNNING,

  /** 备份成功(已转储冷表并导出文件) */
  SUCCEEDED,

  /** 备份失败(数据仍在热表,未丢失) */
  FAILED,
  ;

  companion object {
    fun fromValue(value: String): LogBackupStatus =
      when (value) {
        "RUNNING" -> RUNNING
        "SUCCEEDED" -> SUCCEEDED
        "FAILED" -> FAILED
        else -> throw IllegalArgumentException("Unknown LogBackupStatus value: '" + value + "'")
      }
  }
}
