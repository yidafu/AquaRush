// 转换自 changes/v1.2.4-add-system-security-notification-data-config.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V124AddSystemSecurityNotificationDataConfig :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.2.4-add-system-security-notification-data-config") {
      comment(
        "Seed system config params for F-SYS-004 account & security, F-SYS-005 notification & subscription message, F-SYS-006 data maintenance",
      )
      // ==================== 账号与安全策略参数组(F-SYS-004) ====================
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "16")
        column(name = "setting_key", value = "password_min_length")
        column(name = "setting_value", value = "8")
        column(name = "description", value = "密码最小长度,新创建/重置密码按此校验")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "SECURITY")
        column(name = "default_value", value = "8")
        column(name = "min_value", value = "6")
        column(name = "max_value", value = "32")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "17")
        column(name = "setting_key", value = "password_require_alphanumeric")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "是否要求密码字母数字混合")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "SECURITY")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "18")
        column(name = "setting_key", value = "login_fail_lock_threshold")
        column(name = "setting_value", value = "5")
        column(name = "description", value = "登录失败锁定次数,连续错误达该次数后账号锁定")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "SECURITY")
        column(name = "default_value", value = "5")
        column(name = "min_value", value = "1")
        column(name = "max_value", value = "99")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "19")
        column(name = "setting_key", value = "login_lock_duration_minutes")
        column(name = "setting_value", value = "30")
        column(name = "description", value = "账号锁定持续时间(分钟)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "SECURITY")
        column(name = "default_value", value = "30")
        column(name = "min_value", value = "1")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "20")
        column(name = "setting_key", value = "token_validity_hours")
        column(name = "setting_value", value = "24")
        column(name = "description", value = "Token 有效期(小时),修改后重启生效")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "SECURITY")
        column(name = "default_value", value = "24")
        column(name = "min_value", value = "1")
        column(name = "max_value", value = "720")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      // ==================== 通知与订阅消息参数组(F-SYS-005) ====================
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "21")
        column(name = "setting_key", value = "notification_template_order_created_id")
        column(name = "description", value = "下单通知订阅消息模板 ID(未配置为空,密钥类不在此在线配置 R-SYS-02)")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "22")
        column(name = "setting_key", value = "notification_template_order_created_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "下单通知模板启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "23")
        column(name = "setting_key", value = "notification_template_delivery_completed_id")
        column(name = "description", value = "配送完成订阅消息模板 ID(未配置为空)")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "24")
        column(name = "setting_key", value = "notification_template_delivery_completed_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "配送完成模板启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "25")
        column(name = "setting_key", value = "notification_template_order_urging_id")
        column(name = "description", value = "催单订阅消息模板 ID(未配置为空)")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "26")
        column(name = "setting_key", value = "notification_template_order_urging_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "催单模板启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "27")
        column(name = "setting_key", value = "notification_template_payment_result_id")
        column(name = "description", value = "支付结果订阅消息模板 ID(未配置为空)")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "28")
        column(name = "setting_key", value = "notification_template_payment_result_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "支付结果模板启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      // ==================== 数据维护参数组(F-SYS-006) ====================
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "29")
        column(name = "setting_key", value = "log_retention_api_days")
        column(name = "setting_value", value = "90")
        column(name = "description", value = "API 日志保留天数,超期日志按策略清理")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "DATA_MAINTENANCE")
        column(name = "default_value", value = "90")
        column(name = "min_value", value = "1")
        column(name = "max_value", value = "3650")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "30")
        column(name = "setting_key", value = "log_retention_user_action_days")
        column(name = "setting_value", value = "90")
        column(name = "description", value = "用户操作日志保留天数,超期日志按策略清理")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "DATA_MAINTENANCE")
        column(name = "default_value", value = "90")
        column(name = "min_value", value = "1")
        column(name = "max_value", value = "3650")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "31")
        column(name = "setting_key", value = "log_retention_business_days")
        column(name = "setting_value", value = "90")
        column(name = "description", value = "业务日志保留天数,超期日志按策略清理")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "DATA_MAINTENANCE")
        column(name = "default_value", value = "90")
        column(name = "min_value", value = "1")
        column(name = "max_value", value = "3650")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "32")
        column(name = "setting_key", value = "log_cleanup_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "日志清理开关,关闭后不再按策略自动清理")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "DATA_MAINTENANCE")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
    }
  })
