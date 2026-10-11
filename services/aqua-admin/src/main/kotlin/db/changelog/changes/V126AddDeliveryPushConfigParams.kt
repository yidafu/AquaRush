// 转换自 changes/v1.2.6-add-delivery-push-config-params.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V126AddDeliveryPushConfigParams :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.2.6-add-delivery-push-config-params") {
      comment("Seed delivery app real-time push config params for F-NTF-003 (scenario switches + offline polling interval)")
      // ==================== 送水 App 实时推送配置(F-NTF-003,NOTIFICATION 参数组) ====================
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "33")
        column(name = "setting_key", value = "delivery_push_new_order_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "新订单(可抢单)推送,送水工在线时实时触达")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "34")
        column(name = "setting_key", value = "delivery_push_assigned_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "被派单推送,订单派给送水工时实时触达")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "35")
        column(name = "setting_key", value = "delivery_push_cancelled_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "订单取消推送,已接订单被取消时触达")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "36")
        column(name = "setting_key", value = "delivery_push_urging_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "用户催单推送,用户催单时触达")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "37")
        column(name = "setting_key", value = "delivery_push_timeout_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "配送超时推送,超时未配送时触达")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "38")
        column(name = "setting_key", value = "delivery_push_payment_timeout_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "支付超时推送,待支付订单超时未支付时触达")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "39")
        column(name = "setting_key", value = "delivery_poll_interval_seconds")
        column(name = "setting_value", value = "30")
        column(name = "description", value = "离线/弱网轮询间隔(秒),实时通道断开时降级轮询(R-NTF-03,默认 30)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "30")
        column(name = "min_value", value = "5")
        column(name = "max_value", value = "300")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
    }
  })
