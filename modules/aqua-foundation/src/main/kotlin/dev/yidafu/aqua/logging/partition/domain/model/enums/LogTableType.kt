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

import dev.yidafu.aqua.common.domain.model.SystemSettingKeys

/**
 * 日志表类型(离线备份/分区管理,F-SYS-006)
 * 携带热表名、保留天数配置键与冷表 schema;与 PRD 11-system-settings.md §F-SYS-006 保持一致。
 */
enum class LogTableType(
  /** 在线热表表名(按月分区父表) */
  val tableName: String,
  /** 数据维护参数组中该类日志的保留天数键 */
  val retentionKey: String,
) {
  /** API 请求日志(api_logs) */
  API_LOG("api_logs", SystemSettingKeys.LOG_RETENTION_API_DAYS),
  /** 用户操作日志(user_action_logs) */
  USER_ACTION_LOG("user_action_logs", SystemSettingKeys.LOG_RETENTION_USER_ACTION_DAYS),
  /** 业务日志(business_logs) */
  BUSINESS_LOG("business_logs", SystemSettingKeys.LOG_RETENTION_BUSINESS_DAYS),
  ;
}
