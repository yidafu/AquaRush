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
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.systemconfig.domain.exception.InvalidSettingValueException
import dev.yidafu.aqua.ops.systemconfig.domain.exception.SettingKeyNotFoundException
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 系统参数变更服务单元测试(R-SYS-01:类型/范围校验、跨字段校验、审计留痕)
 */
class SystemConfigMutationServiceImplTest :
  FunSpec({
    var systemSettingsRepository: SystemSettingsRepository = mockk()
    var opsConfigAuditService: OpsConfigAuditService = mockk()
    var service: SystemConfigMutationServiceImpl = mockk()

    fun numberSetting(
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

    fun booleanSetting(
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

    fun enumSetting(
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

    fun stringSetting(
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

    beforeTest {
      systemSettingsRepository = mockk(relaxed = true)
      opsConfigAuditService = mockk(relaxed = true)
      service = SystemConfigMutationServiceImpl(systemSettingsRepository, opsConfigAuditService)
    }
    test("batch save updates values with audit") {
      val low = numberSetting(SystemSettingKeys.LOW_STOCK_THRESHOLD, "10", "0", "99999", "10")
      val mid = numberSetting(SystemSettingKeys.STOCK_WARNING_TIER_MID, "50", "0", "99999", "50")
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.LOW_STOCK_THRESHOLD) } returns low
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.STOCK_WARNING_TIER_MID) } returns mid
      every { systemSettingsRepository.saveAll(any<Iterable<SystemSettingsModel>>()) } answers { firstArg() }

      val saved =
        service.saveSystemConfigs(
          adminId = 1L,
          adminName = "admin",
          items =
            listOf(
              SaveSystemConfigItemRequest(SystemSettingKeys.LOW_STOCK_THRESHOLD, "20"),
              SaveSystemConfigItemRequest(SystemSettingKeys.STOCK_WARNING_TIER_MID, "80"),
            ),
        )

      saved[0].settingValue shouldBe "20"
      saved[1].settingValue shouldBe "80"
      saved[0].updatedBy shouldBe "admin"
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
    test("number out of range rejected") {
      val setting = numberSetting(SystemSettingKeys.LOW_STOCK_THRESHOLD, "10", "0", "99999", "10")
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.LOW_STOCK_THRESHOLD) } returns setting

      val ex =
        shouldThrow<InvalidSettingValueException> {
          service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest(SystemSettingKeys.LOW_STOCK_THRESHOLD, "100000")))
        }
      ex.message shouldBe "参数值超出范围"
    }
    test("number format rejected") {
      val setting = numberSetting(SystemSettingKeys.LOW_STOCK_THRESHOLD, "10", "0", "99999", "10")
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.LOW_STOCK_THRESHOLD) } returns setting

      shouldThrow<InvalidSettingValueException> {
        service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest(SystemSettingKeys.LOW_STOCK_THRESHOLD, "abc")))
      }
    }
    test("boolean invalid value rejected") {
      val setting = booleanSetting(SystemSettingKeys.DELIVERY_SLOT_MORNING_ENABLED, "true")
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.DELIVERY_SLOT_MORNING_ENABLED) } returns setting

      val ex =
        shouldThrow<InvalidSettingValueException> {
          service.saveSystemConfigs(
            1L,
            "admin",
            listOf(SaveSystemConfigItemRequest(SystemSettingKeys.DELIVERY_SLOT_MORNING_ENABLED, "yes")),
          )
        }
      ex.message shouldBe "参数值格式不正确"
    }
    test("enum value not in options rejected") {
      val setting = enumSetting("some_enum", "A", "A,B,C")
      every { systemSettingsRepository.findBySettingKey("some_enum") } returns setting

      shouldThrow<InvalidSettingValueException> {
        service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest("some_enum", "D")))
      }
    }
    test("stock tiers must increase") {
      val low = numberSetting(SystemSettingKeys.LOW_STOCK_THRESHOLD, "10", "0", "99999", "10")
      val mid = numberSetting(SystemSettingKeys.STOCK_WARNING_TIER_MID, "50", "0", "99999", "50")
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.LOW_STOCK_THRESHOLD) } returns low
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.STOCK_WARNING_TIER_MID) } returns mid

      val ex =
        shouldThrow<InvalidSettingValueException> {
          service.saveSystemConfigs(
            1L,
            "admin",
            listOf(
              SaveSystemConfigItemRequest(SystemSettingKeys.LOW_STOCK_THRESHOLD, "20"),
              SaveSystemConfigItemRequest(SystemSettingKeys.STOCK_WARNING_TIER_MID, "10"),
            ),
          )
        }
      ex.message shouldBe "档位需递增"
    }
    test("time window must be legal") {
      val start = stringSetting(SystemSettingKeys.DELIVERY_SLOT_MORNING_START, "09:00")
      val end = stringSetting(SystemSettingKeys.DELIVERY_SLOT_MORNING_END, "12:00")
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.DELIVERY_SLOT_MORNING_START) } returns start
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.DELIVERY_SLOT_MORNING_END) } returns end

      val ex =
        shouldThrow<InvalidSettingValueException> {
          service.saveSystemConfigs(
            1L,
            "admin",
            listOf(
              SaveSystemConfigItemRequest(SystemSettingKeys.DELIVERY_SLOT_MORNING_START, "12:00"),
              SaveSystemConfigItemRequest(SystemSettingKeys.DELIVERY_SLOT_MORNING_END, "09:00"),
            ),
          )
        }
      ex.message shouldBe "时间窗不合法"
    }
    test("unknown key rejected") {
      every { systemSettingsRepository.findBySettingKey("not_exist_key") } returns null as SystemSettingsModel?

      shouldThrow<SettingKeyNotFoundException> {
        service.saveSystemConfigs(1L, "admin", listOf(SaveSystemConfigItemRequest("not_exist_key", "1")))
      }
    }
    test("restore resets to default with audit") {
      val setting = numberSetting(SystemSettingKeys.LOW_STOCK_THRESHOLD, "30", "0", "99999", "10")
      every { systemSettingsRepository.findBySettingKey(SystemSettingKeys.LOW_STOCK_THRESHOLD) } returns setting
      every { systemSettingsRepository.save(any()) } answers { firstArg() }

      val saved = service.restoreSystemConfig(SystemSettingKeys.LOW_STOCK_THRESHOLD, 1L, "admin")

      saved.settingValue shouldBe "10"
      saved.updatedBy shouldBe "admin"
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
    test("restore with null default clears value") {
      val setting = numberSetting("some_key", "30")
      every { systemSettingsRepository.findBySettingKey("some_key") } returns setting
      every { systemSettingsRepository.save(any()) } answers { firstArg() }

      val saved = service.restoreSystemConfig("some_key", 1L, "admin")

      saved.settingValue.shouldBeNull()
    }
  })
