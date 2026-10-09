// 转换自 changes/v1.0.7-add-ops-notice-store-delivery-fee.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
// 存量漂移修复:冗余 addColumn(已被就地并入 v1.0.0)已移除,原因:orders.delivery_fee_cents/addresses.floor 已被就地并入 v1.0.0
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V107AddOpsNoticeStoreDeliveryFee :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.0.7-add-ops-notice-store-delivery-fee") {
      comment(
        "Add ops config tables (notices, store_profiles, delivery_fee_rules) and delivery fee snapshot for F-OPS-002/F-OPS-004/F-OPS-005",
      )
      // 公告表(F-OPS-002)
      createTable(tableName = "notices") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "title", type = "varchar(50)") {
          constraints(nullable = false)
        }
        column(name = "content", type = "text") {
          constraints(nullable = false)
        }
        column(name = "type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "pinned", type = "boolean", defaultValueBoolean = false) {
          constraints(nullable = false)
        }
        column(name = "start_at", type = "timestamp")
        column(name = "end_at", type = "timestamp")
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "published_at", type = "timestamp")
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
      createIndex(tableName = "notices", indexName = "idx_notices_pinned_published_at") {
        column(name = "pinned")
        column(name = "published_at")
      }
      // 店铺信息配置表(F-OPS-004,单行配置 id=1)
      createTable(tableName = "store_profiles") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "shop_name", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "logo_url", type = "varchar(1000)")
        column(name = "service_hotline", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "business_hours_start", type = "time")
        column(name = "business_hours_end", type = "time")
        column(name = "shop_intro", type = "varchar(100)")
        column(name = "about_us", type = "text")
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
      // 配送费规则配置表(F-OPS-005,单行配置 id=1;楼层口径 R-MON-02)
      createTable(tableName = "delivery_fee_rules") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "free_floor_limit", type = "int", defaultValueNumeric = 4) {
          constraints(nullable = false)
        }
        column(name = "surcharge_cents", type = "bigint", defaultValueNumeric = 200) {
          constraints(nullable = false)
        }
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
    }
  })
