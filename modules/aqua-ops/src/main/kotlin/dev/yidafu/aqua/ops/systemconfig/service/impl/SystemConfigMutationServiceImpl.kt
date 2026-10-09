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

import dev.yidafu.aqua.api.service.ops.SystemConfigMutationService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import dev.yidafu.aqua.common.graphql.generated.SaveSystemConfigInput
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.systemconfig.domain.exception.InvalidSettingValueException
import dev.yidafu.aqua.ops.systemconfig.domain.exception.SettingKeyNotFoundException
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

/**
 * 系统参数变更服务实现(管理后台)
 * 与 PRD 11-system-settings.md §R-SYS-01 保持一致:值按声明类型/范围校验(数值区间/布尔/枚举/长度),
 * 同批次跨字段校验(库存档位递增、配送时段开始 < 结束),每次修改留审计日志(操作人/时间/旧值/新值),
 * 不追溯影响已生成业务数据。
 */
@Service
class SystemConfigMutationServiceImpl(
  private val systemSettingsRepository: SystemSettingsRepository,
  private val opsConfigAuditService: OpsConfigAuditService,
) : SystemConfigMutationService {
  @Transactional
  override fun saveSystemConfigs(
    adminId: Long,
    adminName: String,
    items: List<SaveSystemConfigInput>,
  ): List<SystemSettingsModel> {
    val settings =
      items.map { item ->
        val setting =
          systemSettingsRepository.findBySettingKey(item.settingKey)
            ?: throw SettingKeyNotFoundException(item.settingKey)
        setting to item
      }

    val validatedValues =
      settings.associate { (setting, item) -> setting.settingKey to validateValue(setting, item.settingValue) }
    validateCrossFieldRules(validatedValues)

    val changed = linkedMapOf<String, Pair<String?, String?>>()
    settings.forEach { (setting, item) ->
      val newValue = validatedValues.getValue(setting.settingKey)
      val oldValue = setting.settingValue
      if (oldValue != newValue) {
        setting.settingValue = newValue
        setting.updatedBy = adminName
        changed[setting.settingKey] = oldValue to newValue
      }
    }
    systemSettingsRepository.saveAll(settings.map { it.first })

    if (changed.isNotEmpty()) {
      opsConfigAuditService.record(
        targetType = OpsConfigAuditService.TARGET_SYSTEM_CONFIG,
        targetId = null,
        actionType = OpsConfigAuditService.ACTION_SYS_CONFIG_SAVE,
        operatorId = adminId,
        operatorName = adminName,
        before = changed.mapValues { it.value.first },
        after = changed.mapValues { it.value.second },
      )
    }
    return settings.map { it.first }
  }

  @Transactional
  override fun restoreSystemConfig(
    settingKey: String,
    adminId: Long,
    adminName: String,
  ): SystemSettingsModel {
    val setting =
      systemSettingsRepository.findBySettingKey(settingKey)
        ?: throw SettingKeyNotFoundException(settingKey)
    val oldValue = setting.settingValue
    val newValue = setting.defaultValue
    setting.settingValue = newValue
    setting.updatedBy = adminName
    val saved = systemSettingsRepository.save(setting)

    opsConfigAuditService.record(
      targetType = OpsConfigAuditService.TARGET_SYSTEM_CONFIG,
      targetId = null,
      actionType = OpsConfigAuditService.ACTION_SYS_CONFIG_RESTORE,
      operatorId = adminId,
      operatorName = adminName,
      before = mapOf(settingKey to oldValue),
      after = mapOf(settingKey to newValue),
    )
    return saved
  }

  /**
   * 按参数声明类型校验值(R-SYS-01:数值区间/布尔/枚举/长度)
   */
  private fun validateValue(
    setting: SystemSettingsModel,
    value: String?,
  ): String {
    val v = value ?: throw InvalidSettingValueException("参数值格式不正确")
    return when (setting.settingType) {
      SystemSettingType.NUMBER -> {
        val num = v.toLongOrNull() ?: throw InvalidSettingValueException("参数值格式不正确")
        setting.minValue?.toLongOrNull()?.let { if (num < it) throw InvalidSettingValueException("参数值超出范围") }
        setting.maxValue?.toLongOrNull()?.let { if (num > it) throw InvalidSettingValueException("参数值超出范围") }
        v
      }
      SystemSettingType.BOOLEAN -> {
        if (v != "true" && v != "false") throw InvalidSettingValueException("参数值格式不正确")
        v
      }
      SystemSettingType.STRING -> {
        if (v.length > 255) throw InvalidSettingValueException("参数值格式不正确")
        v
      }
      SystemSettingType.ENUM -> {
        val allowed =
          setting.options
            ?.split(",")
            ?.map { it.trim() }
            .orEmpty()
        if (v !in allowed) throw InvalidSettingValueException("参数值格式不正确")
        v
      }
    }
  }

  /**
   * 同批次跨字段校验:
   * - 库存档位:低库存阈值 < 中档阈值(PRD F-SYS-003 「档位需递增」)
   * - 配送时段:开始 < 结束(PRD F-SYS-002 「时间窗不合法」)
   */
  private fun validateCrossFieldRules(values: Map<String, String>) {
    val low = values[SystemSettingKeys.LOW_STOCK_THRESHOLD]?.toLongOrNull()
    val mid = values[SystemSettingKeys.STOCK_WARNING_TIER_MID]?.toLongOrNull()
    if (low != null && mid != null && low >= mid) {
      throw InvalidSettingValueException("档位需递增")
    }

    listOf(
      SystemSettingKeys.DELIVERY_SLOT_MORNING_START to SystemSettingKeys.DELIVERY_SLOT_MORNING_END,
      SystemSettingKeys.DELIVERY_SLOT_AFTERNOON_START to SystemSettingKeys.DELIVERY_SLOT_AFTERNOON_END,
      SystemSettingKeys.DELIVERY_SLOT_EVENING_START to SystemSettingKeys.DELIVERY_SLOT_EVENING_END,
    ).forEach { (startKey, endKey) ->
      val start = values[startKey]
      val end = values[endKey]
      if (start != null && end != null) {
        val startTime = runCatching { LocalTime.parse(start) }.getOrElse { throw InvalidSettingValueException("时间窗不合法") }
        val endTime = runCatching { LocalTime.parse(end) }.getOrElse { throw InvalidSettingValueException("时间窗不合法") }
        if (!startTime.isBefore(endTime)) {
          throw InvalidSettingValueException("时间窗不合法")
        }
      }
    }
  }
}
