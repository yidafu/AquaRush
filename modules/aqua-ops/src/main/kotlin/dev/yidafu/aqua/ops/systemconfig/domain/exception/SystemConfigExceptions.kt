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

package dev.yidafu.aqua.ops.systemconfig.domain.exception

import dev.yidafu.aqua.common.graphql.validation.ValidationException

/**
 * 系统参数键不存在异常(R-SYS-01:仅白名单种子参数可在线修改)
 */
class SettingKeyNotFoundException(
  settingKey: String,
) : RuntimeException("System setting not found: $settingKey")

/**
 * 参数值非法异常
 * 校验文案与 PRD 11-system-settings.md §F-SYS-001~003 保持一致(参数值格式不正确/参数值超出范围/档位需递增/时间窗不合法);
 * 继承 ValidationException 使文案经全局 ValidationExceptionHandler 透出到 GraphQL errors。
 */
class InvalidSettingValueException(
  message: String,
) : ValidationException(message)
