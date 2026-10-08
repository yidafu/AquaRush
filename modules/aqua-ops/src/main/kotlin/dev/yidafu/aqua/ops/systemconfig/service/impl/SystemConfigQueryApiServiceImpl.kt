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

package dev.yidafu.aqua.ops.systemconfig.service.impl

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import org.springframework.stereotype.Service

/**
 * 系统参数业务读取服务实现(跨模块消费)
 * 取值优先级:当前值 → 声明默认值 → 调用方兜底值(R-SYS-01 配置中心降级默认)。
 */
@Service
class SystemConfigQueryApiServiceImpl(
  private val systemSettingsRepository: SystemSettingsRepository,
) : SystemConfigQueryApiService {
  override fun getLongValue(
    key: String,
    fallback: Long,
  ): Long {
    val raw = rawValue(key)
    return raw?.toLongOrNull() ?: fallback
  }

  override fun getBooleanValue(
    key: String,
    fallback: Boolean,
  ): Boolean =
    when (rawValue(key)) {
      "true" -> true
      "false" -> false
      else -> fallback
    }

  override fun getStringValue(
    key: String,
    fallback: String?,
  ): String? = rawValue(key) ?: fallback

  private fun rawValue(key: String): String? {
    val setting = systemSettingsRepository.findBySettingKey(key) ?: return null
    return setting.settingValue ?: setting.defaultValue
  }
}
