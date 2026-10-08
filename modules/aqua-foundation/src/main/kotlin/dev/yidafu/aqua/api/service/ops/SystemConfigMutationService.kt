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

package dev.yidafu.aqua.api.service.ops

import dev.yidafu.aqua.api.dto.SaveSystemConfigItemRequest
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel

/**
 * 系统参数变更服务接口(管理后台)
 * 与 PRD 11-system-settings.md §R-SYS-01 保持一致:按声明类型/范围校验,即时生效,每次修改留审计日志;
 * 操作人显式传参,由实现落审计日志。仅超级管理员可调用(SYSTEM_CONFIG_WRITE)。
 */
interface SystemConfigMutationService {
  /**
   * 批量保存系统参数(按声明类型校验;同批次跨字段校验:低库存阈值 < 中档阈值、配送时段开始 < 结束)
   */
  fun saveSystemConfigs(
    adminId: Long,
    adminName: String,
    items: List<SaveSystemConfigItemRequest>,
  ): List<SystemSettingsModel>

  /**
   * 恢复参数默认值(值回 defaultValue,留审计日志)
   */
  fun restoreSystemConfig(
    settingKey: String,
    adminId: Long,
    adminName: String,
  ): SystemSettingsModel
}
