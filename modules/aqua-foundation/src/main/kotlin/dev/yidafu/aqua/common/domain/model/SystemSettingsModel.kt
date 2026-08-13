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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 系统配置实体
 * 与 PRD 11-system-settings.md §R-SYS-01 保持一致:平台级「键 + 值 + 类型 + 分组」参数,
 * 仅超级管理员可读写(SYSTEM_CONFIG_READ/WRITE),每次修改留审计日志,不追溯影响已生成业务数据。
 */
@Entity
@Table(name = "system_settings")
data class SystemSettingsModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "setting_key", nullable = false, unique = true)
  val settingKey: String = "",
  @Column(name = "setting_value")
  var settingValue: String? = null,
  @Column(name = "setting_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  var settingType: SystemSettingType = SystemSettingType.STRING,
  @Column(name = "setting_group", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  var settingGroup: SystemSettingGroup = SystemSettingGroup.ORDER_AND_FULFILLMENT,
  @Column(name = "default_value")
  val defaultValue: String? = null,
  @Column(name = "min_value", length = 64)
  val minValue: String? = null,
  @Column(name = "max_value", length = 64)
  val maxValue: String? = null,
  @Column(name = "options", length = 255)
  val options: String? = null,
  @Column(name = "updated_by", length = 50)
  var updatedBy: String? = null,
  @Column(name = "description")
  val description: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }
}

/**
 * 系统配置键常量
 * 键名规则:R-SYS-01(字母数字下划线,唯一);种子数据见 Liquibase v1.0.8-add-system-config-params.xml。
 */
object SystemSettingKeys {
  // 订单与履约参数组(F-SYS-002)
  const val PAYMENT_TIMEOUT_MINUTES = "payment_timeout_minutes" // 支付超时(分钟),超时自动取消订单并恢复库存(R-ORD-04)
  const val DELIVERY_SLOT_IMMEDIATE_MINUTES = "delivery_slot_immediate_minutes" // 立即配送时限(分钟)
  const val DELIVERY_SLOT_MORNING_ENABLED = "delivery_slot_morning_enabled"
  const val DELIVERY_SLOT_MORNING_START = "delivery_slot_morning_start"
  const val DELIVERY_SLOT_MORNING_END = "delivery_slot_morning_end"
  const val DELIVERY_SLOT_AFTERNOON_ENABLED = "delivery_slot_afternoon_enabled"
  const val DELIVERY_SLOT_AFTERNOON_START = "delivery_slot_afternoon_start"
  const val DELIVERY_SLOT_AFTERNOON_END = "delivery_slot_afternoon_end"
  const val DELIVERY_SLOT_EVENING_ENABLED = "delivery_slot_evening_enabled"
  const val DELIVERY_SLOT_EVENING_START = "delivery_slot_evening_start"
  const val DELIVERY_SLOT_EVENING_END = "delivery_slot_evening_end"
  const val DISPATCH_TIMEOUT_REMINDER_MINUTES = "dispatch_timeout_reminder_minutes" // 派单超时提醒(分钟),默认值待 PRD 确认
  const val AUTO_CONFIRM_RECEIPT_DAYS = "auto_confirm_receipt_days" // 自动确认收货(天),默认值待 PRD 确认

  // 库存预警参数组(F-SYS-003,R-PRO-02)
  const val LOW_STOCK_THRESHOLD = "low_stock_threshold" // 低库存阈值(件),默认 10
  const val STOCK_WARNING_TIER_MID = "stock_warning_tier_mid" // 中档预警阈值(件),默认 50;须大于低库存阈值
}
