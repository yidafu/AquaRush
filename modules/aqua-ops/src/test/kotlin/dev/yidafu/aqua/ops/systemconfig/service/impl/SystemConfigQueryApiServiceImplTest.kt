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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * 系统参数业务读取服务单元测试(R-SYS-01:当前值 → 默认值 → 调用方兜底)
 */
class SystemConfigQueryApiServiceImplTest :
  FunSpec({
    var systemSettingsRepository: SystemSettingsRepository = mockk()
    var service: SystemConfigQueryApiServiceImpl = mockk()

    fun setting(
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

    beforeTest {
      systemSettingsRepository = mockk(relaxed = true)
      service = SystemConfigQueryApiServiceImpl(systemSettingsRepository)
    }
    test("reads current value") {
      every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting("low_stock_threshold", "20", "10")

      service.getLongValue("low_stock_threshold", 10L) shouldBe 20L
      service.getBooleanValue("flag", true) shouldBe true // 未配置回退
    }
    test("falls back to default value") {
      every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting("low_stock_threshold", null, "10")

      service.getLongValue("low_stock_threshold", 99L) shouldBe 10L
    }
    test("falls back to caller fallback") {
      every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting("low_stock_threshold", "abc", null)

      service.getLongValue("low_stock_threshold", 99L) shouldBe 99L
    }
    test("missing row falls back") {
      every { systemSettingsRepository.findBySettingKey("not_exist") } returns null as SystemSettingsModel?

      service.getLongValue("not_exist", 99L) shouldBe 99L
      service.getStringValue("not_exist", "x") shouldBe "x"
      service.getStringValue("not_exist", null).shouldBeNull()
    }
    test("boolean values parsed") {
      every { systemSettingsRepository.findBySettingKey("delivery_slot_morning_enabled") } returns
        setting("delivery_slot_morning_enabled", "false", "true")

      service.getBooleanValue("delivery_slot_morning_enabled", true) shouldBe false
    }
  })
