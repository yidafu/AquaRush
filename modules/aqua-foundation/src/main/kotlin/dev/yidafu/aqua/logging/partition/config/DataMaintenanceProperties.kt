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

package dev.yidafu.aqua.logging.partition.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 数据维护配置属性(F-SYS-006)
 * 仅存放离线备份的基础设施配置(备份文件目录,不暴露为在线系统参数)。
 */
@ConfigurationProperties(prefix = "aqua.data-maintenance")
data class DataMaintenanceProperties(
  /** 离线备份文件输出目录(默认 ./data/log-backup,可用环境变量 LOG_BACKUP_DIR 覆盖) */
  val backupDir: String = "./data/log-backup",
)
