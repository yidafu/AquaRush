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

import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 系统参数业务读取服务单元测试(R-SYS-01:当前值 → 默认值 → 调用方兜底)
 */
class SystemConfigQueryApiServiceImplTest {
  private lateinit var systemSettingsRepository: SystemSettingsRepository
  private lateinit var service: SystemConfigQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    systemSettingsRepository = mockk(relaxed = true)
    service = SystemConfigQueryApiServiceImpl(systemSettingsRepository)
  }

  private fun setting(
    key: String,
    value: String?,
    defaultValue: String?,
  ) = SystemSettingsModel(
    id = 1L,
    settingKey = key,
    settingValue = value,
    settingType = SystemSettingType.NUMBER,
    settingGroup = SystemSettingGroup.INVENTORY,
    defaultValue = defaultValue,
  )

  @Test
  @DisplayName("读取当前值")
  fun `reads current value`() {
    every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting("low_stock_threshold", "20", "10")

    assertEquals(20L, service.getLongValue("low_stock_threshold", 10L))
    assertEquals(true, service.getBooleanValue("flag", true)) // 未配置回退
  }

  @Test
  @DisplayName("当前值为空回退默认值")
  fun `falls back to default value`() {
    every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting("low_stock_threshold", null, "10")

    assertEquals(10L, service.getLongValue("low_stock_threshold", 99L))
  }

  @Test
  @DisplayName("当前值与默认值均不可解析回退调用方兜底值")
  fun `falls back to caller fallback`() {
    every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting("low_stock_threshold", "abc", null)

    assertEquals(99L, service.getLongValue("low_stock_threshold", 99L))
  }

  @Test
  @DisplayName("行不存在回退调用方兜底值")
  fun `missing row falls back`() {
    every { systemSettingsRepository.findBySettingKey("not_exist") } returns null as SystemSettingsModel?

    assertEquals(99L, service.getLongValue("not_exist", 99L))
    assertEquals("x", service.getStringValue("not_exist", "x"))
    assertNull(service.getStringValue("not_exist", null))
  }

  @Test
  @DisplayName("布尔取值按 true/false 解析")
  fun `boolean values parsed`() {
    every { systemSettingsRepository.findBySettingKey("delivery_slot_morning_enabled") } returns
      setting("delivery_slot_morning_enabled", "false", "true")

    assertEquals(false, service.getBooleanValue("delivery_slot_morning_enabled", true))
  }
}
