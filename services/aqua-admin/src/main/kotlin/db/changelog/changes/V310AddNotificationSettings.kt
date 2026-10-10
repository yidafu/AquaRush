// 手工新增(JPA→Exposed 迁移 P5):补回历史 Hibernate ddl-auto 建出、Liquibase changelog 中缺失的表。
// changeSet id/author 沿用本仓命名；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V310AddNotificationSettings :
  KotlinCompiledDatabaseChangeLog({
    // =======================================================================
    // notification_settings(用户通知开关,F-USER-007)
    //
    // 背景:`NotificationSettingsModel` / Exposed `NotificationSettingsTable` / 仓储一直读写该表,
    // 但 38 个 XML changelog 里从来没有它的建表语句 —— 历史环境的表是 Hibernate `ddl-auto` 建的,
    // 于是「全新库按 Liquibase 迁移」的 schema 缺表,后台创建用户(写入默认通知开关)直接失败。
    // P5 收口补齐 DDL,列逐字对齐实体与 Exposed 表定义。
    //
    // 说明:
    // 1. 不建 `user_id → users(id)` 外键 —— 历史 Hibernate 表按裸 Long 映射,本就没有外键;
    //    补 FK 会让存量数据(可能含已删用户)在新环境迁移失败。
    // 2. 存量库已存在该表 → preConditions 守卫 MARK_RAN,保证同一 changeSet 在
    //    「全新库」与「已有 ddl-auto 库」上都可执行。
    // =======================================================================
    changeSet(author = "aqua-rush", id = "3.1.0-add-notification-settings") {
      preConditions(onFail = "MARK_RAN", onErrorMessage = "notification_settings 已存在,跳过") {
        not {
          tableExists(tableName = "notification_settings")
        }
      }
      comment("Create notification_settings table (missing from historical Liquibase changelog, built by Hibernate ddl-auto)")
      createTable(tableName = "notification_settings") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "order_updates", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "payment_notifications", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "delivery_notifications", type = "boolean", defaultValueBoolean = true) {
          constraints(nullable = false)
        }
        column(name = "promotional_notifications", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        // SoftDeletableTable 三列(Hibernate @SoftDelete 语义)
        column(name = "is_deleted", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
      }
      createIndex(tableName = "notification_settings", indexName = "idx_notification_settings_user_id") {
        column(name = "user_id")
      }
    }
  })
