/**
 * 系统配置键常量(F-SYS-002 ~ F-SYS-006)
 *
 * 与后端 `SystemSettingKeys`(modules/aqua-foundation/.../common/domain/model/SystemSettingsModel.kt)保持一致:
 * 键名规则 R-SYS-01(字母数字下划线,唯一);种子数据见 Liquibase v1.0.8-add-system-config-params.xml。
 * 前端引用系统配置 settingKey 一律使用此常量,禁止写死字符串。
 */
export const SystemSettingKeys = {
  // 订单与履约参数组(F-SYS-002)
  PAYMENT_TIMEOUT_MINUTES: 'payment_timeout_minutes', // 支付超时(分钟),超时自动取消订单并恢复库存(R-ORD-04)
  DELIVERY_SLOT_IMMEDIATE_MINUTES: 'delivery_slot_immediate_minutes', // 立即配送时限(分钟)
  DELIVERY_SLOT_MORNING_ENABLED: 'delivery_slot_morning_enabled',
  DELIVERY_SLOT_MORNING_START: 'delivery_slot_morning_start',
  DELIVERY_SLOT_MORNING_END: 'delivery_slot_morning_end',
  DELIVERY_SLOT_AFTERNOON_ENABLED: 'delivery_slot_afternoon_enabled',
  DELIVERY_SLOT_AFTERNOON_START: 'delivery_slot_afternoon_start',
  DELIVERY_SLOT_AFTERNOON_END: 'delivery_slot_afternoon_end',
  DELIVERY_SLOT_EVENING_ENABLED: 'delivery_slot_evening_enabled',
  DELIVERY_SLOT_EVENING_START: 'delivery_slot_evening_start',
  DELIVERY_SLOT_EVENING_END: 'delivery_slot_evening_end',
  DISPATCH_TIMEOUT_REMINDER_MINUTES: 'dispatch_timeout_reminder_minutes', // 派单超时提醒(分钟)
  AUTO_CONFIRM_RECEIPT_DAYS: 'auto_confirm_receipt_days', // 自动确认收货(天)

  // 库存预警参数组(F-SYS-003,R-PRO-02)
  LOW_STOCK_THRESHOLD: 'low_stock_threshold', // 低库存阈值(件),默认 10
  STOCK_WARNING_TIER_MID: 'stock_warning_tier_mid', // 中档预警阈值(件),默认 50;须大于低库存阈值

  // 账号与安全策略参数组(F-SYS-004)
  PASSWORD_MIN_LENGTH: 'password_min_length', // 密码最小长度,默认 8
  PASSWORD_REQUIRE_ALPHANUMERIC: 'password_require_alphanumeric', // 是否要求密码字母数字混合
  LOGIN_FAIL_LOCK_THRESHOLD: 'login_fail_lock_threshold', // 登录失败锁定次数,默认 5
  LOGIN_LOCK_DURATION_MINUTES: 'login_lock_duration_minutes', // 锁定时长(分钟)
  TOKEN_VALIDITY_HOURS: 'token_validity_hours', // Token 有效期(小时,重启生效提示)

  // 通知与订阅消息参数组(F-SYS-005):每场景模板 ID(STRING,未配置为空)+ 启用开关(BOOLEAN)
  NOTIFICATION_TEMPLATE_ORDER_CREATED_ID: 'notification_template_order_created_id', // 下单通知模板 ID
  NOTIFICATION_TEMPLATE_ORDER_CREATED_ENABLED: 'notification_template_order_created_enabled', // 下单通知启用
  NOTIFICATION_TEMPLATE_DELIVERY_COMPLETED_ID: 'notification_template_delivery_completed_id', // 配送完成模板 ID
  NOTIFICATION_TEMPLATE_DELIVERY_COMPLETED_ENABLED: 'notification_template_delivery_completed_enabled', // 配送完成启用
  NOTIFICATION_TEMPLATE_ORDER_URGING_ID: 'notification_template_order_urging_id', // 催单模板 ID
  NOTIFICATION_TEMPLATE_ORDER_URGING_ENABLED: 'notification_template_order_urging_enabled', // 催单启用
  NOTIFICATION_TEMPLATE_PAYMENT_RESULT_ID: 'notification_template_payment_result_id', // 支付结果模板 ID
  NOTIFICATION_TEMPLATE_PAYMENT_RESULT_ENABLED: 'notification_template_payment_result_enabled', // 支付结果启用

  // 送水 App 实时推送配置(F-NTF-003):推送场景启用开关 + 离线/弱网轮询间隔(R-NTF-03,默认 30s)
  DELIVERY_PUSH_NEW_ORDER_ENABLED: 'delivery_push_new_order_enabled', // 新订单(可抢单)推送
  DELIVERY_PUSH_ASSIGNED_ENABLED: 'delivery_push_assigned_enabled', // 被派单推送
  DELIVERY_PUSH_CANCELLED_ENABLED: 'delivery_push_cancelled_enabled', // 订单取消推送
  DELIVERY_PUSH_URGING_ENABLED: 'delivery_push_urging_enabled', // 用户催单推送
  DELIVERY_PUSH_TIMEOUT_ENABLED: 'delivery_push_timeout_enabled', // 配送超时推送
  DELIVERY_PUSH_PAYMENT_TIMEOUT_ENABLED: 'delivery_push_payment_timeout_enabled', // 支付超时推送
  DELIVERY_POLL_INTERVAL_SECONDS: 'delivery_poll_interval_seconds', // 离线/弱网轮询间隔(秒),默认 30

  // 数据维护参数组(F-SYS-006)
  LOG_RETENTION_API_DAYS: 'log_retention_api_days', // API 日志保留天数,默认 90
  LOG_RETENTION_USER_ACTION_DAYS: 'log_retention_user_action_days', // 用户操作日志保留天数,默认 90
  LOG_RETENTION_BUSINESS_DAYS: 'log_retention_business_days', // 业务日志保留天数,默认 90
  LOG_CLEANUP_ENABLED: 'log_cleanup_enabled', // 日志清理开关
} as const

export type SystemSettingKey = (typeof SystemSettingKeys)[keyof typeof SystemSettingKeys]
