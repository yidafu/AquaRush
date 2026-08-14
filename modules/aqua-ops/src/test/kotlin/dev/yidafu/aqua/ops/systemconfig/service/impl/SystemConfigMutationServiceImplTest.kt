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

import dev.yidafu.aqua.api.dto.SaveSystemConfigItemRequest
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.systemconfig.domain.exception.InvalidSettingValueException
import dev.yidafu.aqua.ops.systemconfig.domain.exception.SettingKeyNotFoundException
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 系统参数变更服务单元测试(R-SYS-01:类型/范围校验、跨字段校验、审计留痕)
 */
class SystemConfigMutationServiceImplTest {
  private lateinit var systemSettingsRepository: SystemSettingsRepository
  private lateinit var opsConfigAuditService: OpsConfigAuditService
  private lateinit var service: SystemConfigMutationServiceImpl

  @BeforeEach
  fun setUp() {
    systemSettingsRepository = mockk(relaxed = true)
    opsConfigAuditService = mockk(relaxed = true)
    service = SystemConfigMutationServiceImpl(systemSettingsRepository, opsConfigAuditService)
  }

  private fun numberSetting(
    key: String,
    value: String?,
    min: String? = null,
    max: String? = null,
    defaultValue: String? = null,
  ) = SystemSettingsModel(
    id = 1L,
    settingKey = key,
    settingValue = value,
    settingType = SystemSettingType.NUMBER,
    settingGroup = SystemSettingGroup.INVENTORY,
    defaultValue = defaultValue,
    minValue = min,
    maxValue = max,
  )

  private fun booleanSetting(
    key: String,
    value: String?,
  ) = SystemSettingsModel(
    id = 2L,
    settingKey = key,
    settingValue = value,
    settingType = SystemSettingType.BOOLEAN,
    settingGroup = SystemSettingGroup.ORDER_AND_FULFILLMENT,
    defaultValue = "true",
  )

  private fun enumSetting(
    key: String,
    value: String?,
    options: String,
  ) = SystemSettingsModel(
    id = 3L,
    settingKey = key,
    settingValue = value,
    settingType = SystemSettingType.ENUM,
    settingGroup = SystemSettingGroup.ORDER_AND_FULFILLMENT,
    options = options,
  )

  private fun stringSetting(
    key: String,
    value: String?,
  ) = SystemSettingsModel(
    id = 4L,
    settingKey = key,
    settingValue = value,
    settingType = SystemSettingType.STRING,
    settingGroup = SystemSettingGroup.ORDER_AND_FULFILLMENT,
    defaultValue = "09:00",
  )

  @Test
  @DisplayName("批量保存更新值并落审计(含前后值)")
  fun `batch save updates values with audit`() {
    val low = numberSetting("low_stock_threshold", "10", "0", "99999", "10")
    val mid = numberSetting("stock_warning_tier_mid", "50", "0", "99999", "50")
    every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns low
    every { systemSettingsRepository.findBySettingKey("stock_warning_tier_mid") } returns mid
    every { systemSettingsRepository.saveAll(any<Iterable<SystemSettingsModel>>()) } answers { firstArg() }

    val saved =
      service.saveSystemConfigs(
        adminId = 1L,
        adminName = "admin",
        items =
          listOf(
            SaveSystemConfigItemRequest("low_stock_threshold", "20"),
            SaveSystemConfigItemRequest("stock_warning_tier_mid", "80"),
          ),
      )

    assertEquals("20", saved[0].settingValue)
    assertEquals("80", saved[1].settingValue)
    assertEquals("admin", saved[0].updatedBy)
    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_SYSTEM_CONFIG,
        null,
        OpsConfigAuditService.ACTION_SYS_CONFIG_SAVE,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }

  @Test
  @DisplayName("数值越界抛「参数值超出范围」")
  fun `number out of range rejected`() {
    val setting = numberSetting("low_stock_threshold", "10", "0", "99999", "10")
    every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting

    val ex =
      assertFailsWith<InvalidSettingValueException> {
        service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest("low_stock_threshold", "100000")))
      }
    assertEquals("参数值超出范围", ex.message)
  }

  @Test
  @DisplayName("数值非数字抛「参数值格式不正确」")
  fun `number format rejected`() {
    val setting = numberSetting("low_stock_threshold", "10", "0", "99999", "10")
    every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting

    assertFailsWith<InvalidSettingValueException> {
      service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest("low_stock_threshold", "abc")))
    }
  }

  @Test
  @DisplayName("布尔非法值抛「参数值格式不正确」")
  fun `boolean invalid value rejected`() {
    val setting = booleanSetting("delivery_slot_morning_enabled", "true")
    every { systemSettingsRepository.findBySettingKey("delivery_slot_morning_enabled") } returns setting

    val ex =
      assertFailsWith<InvalidSettingValueException> {
        service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest("delivery_slot_morning_enabled", "yes")))
      }
    assertEquals("参数值格式不正确", ex.message)
  }

  @Test
  @DisplayName("枚举值不在选项中抛「参数值格式不正确」")
  fun `enum value not in options rejected`() {
    val setting = enumSetting("some_enum", "A", "A,B,C")
    every { systemSettingsRepository.findBySettingKey("some_enum") } returns setting

    assertFailsWith<InvalidSettingValueException> {
      service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest("some_enum", "D")))
    }
  }

  @Test
  @DisplayName("库存档位未递增抛「档位需递增」")
  fun `stock tiers must increase`() {
    val low = numberSetting("low_stock_threshold", "10", "0", "99999", "10")
    val mid = numberSetting("stock_warning_tier_mid", "50", "0", "99999", "50")
    every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns low
    every { systemSettingsRepository.findBySettingKey("stock_warning_tier_mid") } returns mid

    val ex =
      assertFailsWith<InvalidSettingValueException> {
        service.saveSystemConfigs(
          1L,
          "admin",
          listOf(
            SaveSystemConfigItemRequest("low_stock_threshold", "20"),
            SaveSystemConfigItemRequest("stock_warning_tier_mid", "10"),
          ),
        )
      }
    assertEquals("档位需递增", ex.message)
  }

  @Test
  @DisplayName("配送时段开始不早于结束抛「时间窗不合法」")
  fun `time window must be legal`() {
    val start = stringSetting("delivery_slot_morning_start", "09:00")
    val end = stringSetting("delivery_slot_morning_end", "12:00")
    every { systemSettingsRepository.findBySettingKey("delivery_slot_morning_start") } returns start
    every { systemSettingsRepository.findBySettingKey("delivery_slot_morning_end") } returns end

    val ex =
      assertFailsWith<InvalidSettingValueException> {
        service.saveSystemConfigs(
          1L,
          "admin",
          listOf(
            SaveSystemConfigItemRequest("delivery_slot_morning_start", "12:00"),
            SaveSystemConfigItemRequest("delivery_slot_morning_end", "09:00"),
          ),
        )
      }
    assertEquals("时间窗不合法", ex.message)
  }

  @Test
  @DisplayName("未知参数键抛 SettingKeyNotFoundException")
  fun `unknown key rejected`() {
    every { systemSettingsRepository.findBySettingKey("not_exist_key") } returns null as SystemSettingsModel?

    assertFailsWith<SettingKeyNotFoundException> {
      service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest("not_exist_key", "1")))
    }
  }

  @Test
  @DisplayName("恢复默认值回 defaultValue 并落审计")
  fun `restore resets to default with audit`() {
    val setting = numberSetting("low_stock_threshold", "30", "0", "99999", "10")
    every { systemSettingsRepository.findBySettingKey("low_stock_threshold") } returns setting
    every { systemSettingsRepository.save(any()) } answers { firstArg() }

    val saved = service.restoreSystemConfig("low_stock_threshold", 1L, "admin")

    assertEquals("10", saved.settingValue)
    assertEquals("admin", saved.updatedBy)
    verify {
      opsConfigAuditService.record(
        OpsConfigAuditService.TARGET_SYSTEM_CONFIG,
        null,
        OpsConfigAuditService.ACTION_SYS_CONFIG_RESTORE,
        1L,
        "admin",
        before = any(),
        after = any(),
        description = any(),
      )
    }
  }

  @Test
  @DisplayName("恢复默认值:无默认值时清空")
  fun `restore with null default clears value`() {
    val setting = numberSetting("some_key", "30")
    every { systemSettingsRepository.findBySettingKey("some_key") } returns setting
    every { systemSettingsRepository.save(any()) } answers { firstArg() }

    val saved = service.restoreSystemConfig("some_key", 1L, "admin")

    assertNull(saved.settingValue)
  }
}
