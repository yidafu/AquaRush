// 转换自 changes/v1.0.6-add-ops-config-tables.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V106AddOpsConfigTables :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.0.6-add-ops-config-tables") {
      comment("Add operations config tables (banners, feedbacks) for F-OPS-001/F-OPS-003")
      // 轮播图(运营位)配置表
      createTable(tableName = "banners") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "image_url", type = "varchar(1000)") {
          constraints(nullable = false)
        }
        column(name = "title", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "subtitle", type = "varchar(100)")
        column(name = "link_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "link_target", type = "varchar(1000)") {
          constraints(nullable = false)
        }
        column(name = "sort_order", type = "int") {
          constraints(nullable = false)
        }
        column(name = "enabled", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "start_at", type = "timestamp")
        column(name = "end_at", type = "timestamp")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "banners", indexName = "idx_banners_sort_order") {
        column(name = "sort_order")
      }
      // 意见反馈表
      createTable(tableName = "feedbacks") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "feedback_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "content", type = "text") {
          constraints(nullable = false)
        }
        column(name = "images", type = "json")
        column(name = "contact_info", type = "varchar(100)")
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "handle_result", type = "text")
        column(name = "handled_by_id", type = "bigint")
        column(name = "handled_by_name", type = "varchar(100)")
        column(name = "handled_at", type = "timestamp")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "deleted_at", type = "timestamp")
        column(name = "deleted_by", type = "bigint")
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "feedbacks", indexName = "idx_feedbacks_status_created_at") {
        column(name = "status")
        column(name = "created_at")
      }
    }
  })
