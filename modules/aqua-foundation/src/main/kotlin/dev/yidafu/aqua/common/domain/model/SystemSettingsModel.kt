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

import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import java.time.LocalDateTime

/**
 * 系统配置实体
 * 与 PRD 11-system-settings.md §R-SYS-01 保持一致:平台级「键 + 值 + 类型 + 分组」参数,
 * 仅超级管理员可读写(SYSTEM_CONFIG_READ/WRITE),每次修改留审计日志,不追溯影响已生成业务数据。
 */
data class SystemSettingsModel(
  var id: Long? = null,
  val settingKey: String = "",
  var settingValue: String? = null,
  var settingType: SystemSettingType = SystemSettingType.STRING,
  var settingGroup: SystemSettingGroup = SystemSettingGroup.ORDER_AND_FULFILLMENT,
  val defaultValue: String? = null,
  val minValue: String? = null,
  val maxValue: String? = null,
  val options: String? = null,
  var updatedBy: String? = null,
  val description: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)

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

  // 接单超时预警阈值(R-NTF-07/F-NTF-008,缺口 G94):「待派单」超阈值无人接单预警管理员,默认 30 分钟
  const val ORDER_ACCEPT_TIMEOUT_MINUTES = "order_accept_timeout_minutes"

  // 开始配送超时预警阈值(R-NTF-07/F-NTF-008,缺口 G94):「待配送」超阈值未开始配送预警管理员,默认 60 分钟
  const val DELIVERY_START_TIMEOUT_MINUTES = "delivery_start_timeout_minutes"

  // 库存预警参数组(F-SYS-003,R-PRO-02)
  const val LOW_STOCK_THRESHOLD = "low_stock_threshold" // 低库存阈值(件),默认 10
  const val STOCK_WARNING_TIER_MID = "stock_warning_tier_mid" // 中档预警阈值(件),默认 50;须大于低库存阈值

  // 账号与安全策略参数组(F-SYS-004)
  const val PASSWORD_MIN_LENGTH = "password_min_length" // 密码最小长度,默认 8
  const val PASSWORD_REQUIRE_ALPHANUMERIC = "password_require_alphanumeric" // 是否要求密码字母数字混合
  const val LOGIN_FAIL_LOCK_THRESHOLD = "login_fail_lock_threshold" // 登录失败锁定次数,默认 5
  const val LOGIN_LOCK_DURATION_MINUTES = "login_lock_duration_minutes" // 锁定时长(分钟)
  const val TOKEN_VALIDITY_HOURS = "token_validity_hours" // Token 有效期(小时,重启生效提示)

  // 通知与订阅消息参数组(F-SYS-005):每场景模板 ID(STRING,未配置为空)+ 启用开关(BOOLEAN)
  const val NOTIFICATION_TEMPLATE_ORDER_CREATED_ID = "notification_template_order_created_id" // 下单通知模板 ID
  const val NOTIFICATION_TEMPLATE_ORDER_CREATED_ENABLED = "notification_template_order_created_enabled" // 下单通知启用
  const val NOTIFICATION_TEMPLATE_DELIVERY_ASSIGNED_ID = "notification_template_delivery_assigned_id" // 配送员接单模板 ID
  const val NOTIFICATION_TEMPLATE_DELIVERY_ASSIGNED_ENABLED = "notification_template_delivery_assigned_enabled" // 配送员接单启用
  const val NOTIFICATION_TEMPLATE_DELIVERY_STARTED_ID = "notification_template_delivery_started_id" // 开始配送模板 ID
  const val NOTIFICATION_TEMPLATE_DELIVERY_STARTED_ENABLED = "notification_template_delivery_started_enabled" // 开始配送启用
  const val NOTIFICATION_TEMPLATE_DELIVERY_COMPLETED_ID = "notification_template_delivery_completed_id" // 配送完成模板 ID
  const val NOTIFICATION_TEMPLATE_DELIVERY_COMPLETED_ENABLED = "notification_template_delivery_completed_enabled" // 配送完成启用
  const val NOTIFICATION_TEMPLATE_ORDER_URGING_ID = "notification_template_order_urging_id" // 催单模板 ID
  const val NOTIFICATION_TEMPLATE_ORDER_URGING_ENABLED = "notification_template_order_urging_enabled" // 催单启用
  const val NOTIFICATION_TEMPLATE_PAYMENT_RESULT_ID = "notification_template_payment_result_id" // 支付结果模板 ID
  const val NOTIFICATION_TEMPLATE_PAYMENT_RESULT_ENABLED = "notification_template_payment_result_enabled" // 支付结果启用
  const val NOTIFICATION_RETENTION_DAYS = "notification_retention_days" // 站内消息保留天数(R-NTF-04,默认 90)

  // 送水 App 实时推送配置(F-NTF-003):任务关键节点推送场景开关 + 离线/弱网轮询间隔(R-NTF-03,默认 30s)
  const val DELIVERY_PUSH_NEW_ORDER_ENABLED = "delivery_push_new_order_enabled" // 新订单(可抢单)推送
  const val DELIVERY_PUSH_ASSIGNED_ENABLED = "delivery_push_assigned_enabled" // 被派单推送
  const val DELIVERY_PUSH_CANCELLED_ENABLED = "delivery_push_cancelled_enabled" // 订单取消推送
  const val DELIVERY_PUSH_URGING_ENABLED = "delivery_push_urging_enabled" // 用户催单推送
  const val DELIVERY_PUSH_TIMEOUT_ENABLED = "delivery_push_timeout_enabled" // 配送超时推送
  const val DELIVERY_PUSH_PAYMENT_TIMEOUT_ENABLED = "delivery_push_payment_timeout_enabled" // 支付超时推送
  const val DELIVERY_POLL_INTERVAL_SECONDS = "delivery_poll_interval_seconds" // 离线/弱网轮询间隔(秒),默认 30

  // 营销参数组(F-SYS 扩展,PRD 10-marketing.md):优惠券即将过期提醒窗口
  const val COUPON_EXPIRING_NOTIFY_DAYS = "coupon_expiring_notify_days" // 优惠券即将过期提醒窗口(天),默认 3
  const val DELIVERY_TIMEOUT_MINUTES = "delivery_timeout_minutes" // 配送超时阈值(分钟),默认 120

  // 数据维护参数组(F-SYS-006)
  const val LOG_RETENTION_API_DAYS = "log_retention_api_days" // API 日志保留天数,默认 90
  const val LOG_RETENTION_USER_ACTION_DAYS = "log_retention_user_action_days" // 用户操作日志保留天数,默认 90
  const val LOG_RETENTION_BUSINESS_DAYS = "log_retention_business_days" // 业务日志保留天数,默认 90
  const val LOG_CLEANUP_ENABLED = "log_cleanup_enabled" // 日志清理开关
}
