// 转换自 changes/v1.1.0-add-marketing-flash-gift-firstorder.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
// 存量漂移修复:冗余 addColumn(已被就地并入 v1.0.0)已移除,原因:orders 首单活动列已被就地并入 v1.0.0
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V110AddMarketingFlashGiftFirstorder :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.1.0-add-marketing-flash-gift-firstorder") {
      comment(
        "Add flash sale, gift activity, first order activity tables and order gift/ first order snapshots for F-MKT-005/008/009 (PRD 10-marketing.md)",
      )
      // 限时特价活动表(F-MKT-005,R-MKT-02 单价链③)
      createTable(tableName = "flash_sale_activities") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "sale_price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "valid_from", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "valid_to", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
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
      createIndex(tableName = "flash_sale_activities", indexName = "idx_flash_sale_product_time") {
        column(name = "product_id")
        column(name = "valid_from")
        column(name = "valid_to")
      }
      // 买赠/满赠活动表(F-MKT-008,v1 单档单赠品)
      createTable(tableName = "gift_activities") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "gift_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "threshold_cents", type = "bigint")
        column(name = "threshold_quantity", type = "int")
        column(name = "scope", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint")
        column(name = "customer_group_id", type = "bigint")
        column(name = "gift_product_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "gift_quantity", type = "int") {
          constraints(nullable = false)
        }
        column(name = "valid_from", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "valid_to", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
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
      createIndex(tableName = "gift_activities", indexName = "idx_gift_activities_active") {
        column(name = "status")
        column(name = "valid_from")
        column(name = "valid_to")
      }
      // 首单立减/新人专享价活动表(F-MKT-009,两形态单表)
      createTable(tableName = "first_order_activities") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "activity_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "threshold_cents", type = "bigint")
        column(name = "discount_amount_cents", type = "bigint")
        column(name = "discount_rate_permille", type = "bigint")
        column(name = "scope", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint")
        column(name = "newcomer_price_cents", type = "bigint")
        column(name = "valid_from", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "valid_to", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
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
      createIndex(tableName = "first_order_activities", indexName = "idx_first_order_active") {
        column(name = "status")
        column(name = "valid_from")
        column(name = "valid_to")
      }
      createIndex(tableName = "first_order_activities", indexName = "idx_first_order_product") {
        column(name = "product_id")
        column(name = "status")
      }
      // 订单赠品行(R-MKT-09,随单快照,不做软删)
      createTable(tableName = "order_gifts") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "order_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "gift_activity_id", type = "bigint")
        column(name = "gift_activity_name", type = "varchar(100)")
        column(name = "product_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "product_name", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "quantity", type = "int") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "order_gifts", indexName = "idx_order_gifts_order") {
        column(name = "order_id")
      }
    }
  })
