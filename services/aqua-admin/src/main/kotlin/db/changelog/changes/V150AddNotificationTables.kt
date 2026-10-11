// 转换自 changes/v1.5.0-add-notification-tables.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V150AddNotificationTables :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.5.0-add-notification-tables") {
      comment(
        "Notification & message domain (PRD 13-notification.md F-NTF-001~006): unified in-app notification table + WeChat subscription auth table",
      )
      // ==================== 站内消息统一落库表(F-NTF-002/004/006,订阅消息发送记录亦落此表) ====================
      createTable(tableName = "aqua_notifications") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "receiver_type", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "user_id", type = "bigint")
        column(name = "worker_id", type = "bigint")
        column(name = "message_type", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "scene", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "title", type = "varchar(200)") {
          constraints(nullable = false)
        }
        column(name = "summary", type = "varchar(500)")
        column(name = "related_no", type = "varchar(64)")
        column(name = "idempotency_key", type = "varchar(128)") {
          constraints(nullable = false, unique = true)
        }
        column(name = "channel_status", type = "varchar(20)", defaultValue = "PENDING") {
          constraints(nullable = false)
        }
        column(name = "wx_message_id", type = "varchar(100)")
        column(name = "error_message", type = "text")
        column(name = "sent_at", type = "timestamp")
        column(name = "read_at", type = "timestamp")
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        // 微信订阅消息模板上下文(v2.6.0,F-NTF-006 失败消息手动重发)
        column(name = "template_scene", type = "varchar(50)") {
          constraints(nullable = true)
        }
        column(name = "template_page", type = "varchar(255)") {
          constraints(nullable = true)
        }
        column(name = "template_fields", type = "text") {
          constraints(nullable = true)
        }
      }
      createIndex(tableName = "aqua_notifications", indexName = "idx_notifications_receiver_user") {
        column(name = "receiver_type")
        column(name = "user_id")
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_notifications", indexName = "idx_notifications_receiver_worker") {
        column(name = "receiver_type")
        column(name = "worker_id")
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_notifications", indexName = "idx_notifications_created_at") {
        column(name = "created_at")
      }
      createIndex(tableName = "aqua_notifications", indexName = "idx_notifications_deleted") {
        column(name = "is_deleted")
      }
      // ==================== 微信订阅消息授权记录(R-NTF-01,一次性模板:授权一次可发一次) ====================
      createTable(tableName = "aqua_user_subscription_auths") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "template_id", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "remaining_count", type = "int", defaultValueNumeric = 1) {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "aqua_user_subscription_auths", indexName = "idx_sub_auth_user_template") {
        column(name = "user_id")
        column(name = "template_id")
      }
      createIndex(tableName = "aqua_user_subscription_auths", indexName = "idx_sub_auth_deleted") {
        column(name = "is_deleted")
      }
      // ==================== 通知域新增系统参数(补齐 F-NTF-001 五节点模板 + R-NTF-04 保留时长) ====================
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "40")
        column(name = "setting_key", value = "notification_template_delivery_assigned_id")
        column(name = "description", value = "配送员接单订阅消息模板 ID(未配置为空)")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "41")
        column(name = "setting_key", value = "notification_template_delivery_assigned_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "配送员接单模板启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "42")
        column(name = "setting_key", value = "notification_template_delivery_started_id")
        column(name = "description", value = "开始配送订阅消息模板 ID(未配置为空)")
        column(name = "setting_type", value = "STRING")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "43")
        column(name = "setting_key", value = "notification_template_delivery_started_enabled")
        column(name = "setting_value", value = "true")
        column(name = "description", value = "开始配送模板启用")
        column(name = "setting_type", value = "BOOLEAN")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "true")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
      insert(tableName = "aqua_system_settings") {
        column(name = "id", value = "44")
        column(name = "setting_key", value = "notification_retention_days")
        column(name = "setting_value", value = "90")
        column(name = "description", value = "站内消息保留天数,超期消息按策略清理(R-NTF-04,默认 90)")
        column(name = "setting_type", value = "NUMBER")
        column(name = "setting_group", value = "NOTIFICATION")
        column(name = "default_value", value = "90")
        column(name = "min_value", value = "1")
        column(name = "max_value", value = "3650")
        column(name = "created_at", value = "NOW()")
        column(name = "updated_at", value = "NOW()")
      }
    }
  })
