// 转换自 changes/v2.1.0-add-barrel-fee.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V210AddBarrelFee :
  KotlinCompiledDatabaseChangeLog({
    // G06 水桶费改造(桶押金 → 一次性塑料水桶费,2026-08):
    //     - barrel_fees:水桶费价目表(按规格,与商品解耦,后台维护);
    //     - barrel_purchases:水桶购买单(独立支付,每用户总限购一次,order_no 前缀 BK 作为支付 out_trade_no);
    //     - barrel_purchase_items:购桶单明细(规格/单价/数量快照,价目变更不影响已生成购桶单)。
    //     原 deposit_records / products.deposit_price_cents / orders.deposit_amount_cents 保留(历史数据),不再写入。
    changeSet(author = "aqua-rush", id = "2.1.0-create-barrel-fees-table") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "barrel_fees 表已存在,跳过") {
        not {
          tableExists(tableName = "barrel_fees")
        }
      }
      comment("Create barrel_fees table (G06 水桶费价目)")
      createTable(tableName = "barrel_fees") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "specification", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)", defaultValue = "ENABLED") {
          constraints(nullable = false)
        }
        column(name = "sort_order", type = "int", defaultValueNumeric = 999) {
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
      createIndex(tableName = "barrel_fees", indexName = "uk_barrel_fees_specification", unique = true) {
        column(name = "specification")
      }
      createIndex(tableName = "barrel_fees", indexName = "idx_barrel_fees_status_sort") {
        column(name = "status")
        column(name = "sort_order")
      }
    }
    changeSet(author = "aqua-rush", id = "2.1.0-create-barrel-purchases-table") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "barrel_purchases 表已存在,跳过") {
        not {
          tableExists(tableName = "barrel_purchases")
        }
      }
      comment("Create barrel_purchases table (G06 水桶购买单)")
      createTable(tableName = "barrel_purchases") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "order_no", type = "varchar(64)") {
          constraints(nullable = false)
        }
        column(name = "total_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "payment_status", type = "varchar(30)", defaultValue = "PENDING") {
          constraints(nullable = false)
        }
        column(name = "transaction_id", type = "varchar(64)")
        column(name = "paid_at", type = "timestamp")
        column(name = "refunded_at", type = "timestamp")
        column(name = "refunded_by", type = "bigint")
        column(name = "refund_remark", type = "varchar(500)")
        column(name = "remark", type = "varchar(500)")
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
      createIndex(tableName = "barrel_purchases", indexName = "uk_barrel_purchases_order_no", unique = true) {
        column(name = "order_no")
      }
      createIndex(tableName = "barrel_purchases", indexName = "idx_barrel_purchases_user_status") {
        column(name = "user_id")
        column(name = "payment_status")
      }
      createIndex(tableName = "barrel_purchases", indexName = "idx_barrel_purchases_paid_at") {
        column(name = "paid_at")
      }
      addForeignKeyConstraint(
        baseTableName = "barrel_purchases",
        baseColumnNames = "user_id",
        referencedTableName = "users",
        referencedColumnNames = "id",
        constraintName = "fk_barrel_purchases_user",
      )
    }
    changeSet(author = "aqua-rush", id = "2.1.0-create-barrel-purchase-items-table") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "barrel_purchase_items 表已存在,跳过") {
        not {
          tableExists(tableName = "barrel_purchase_items")
        }
      }
      comment("Create barrel_purchase_items table (G06 购桶单明细快照)")
      createTable(tableName = "barrel_purchase_items") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "purchase_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "barrel_fee_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "specification", type = "varchar(100)") {
          constraints(nullable = false)
        }
        column(name = "unit_price_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "quantity", type = "int") {
          constraints(nullable = false)
        }
        column(name = "amount_cents", type = "bigint") {
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
      createIndex(tableName = "barrel_purchase_items", indexName = "idx_barrel_purchase_items_purchase") {
        column(name = "purchase_id")
      }
      addForeignKeyConstraint(
        baseTableName = "barrel_purchase_items",
        baseColumnNames = "purchase_id",
        referencedTableName = "barrel_purchases",
        referencedColumnNames = "id",
        constraintName = "fk_barrel_purchase_items_purchase",
      )
      addForeignKeyConstraint(
        baseTableName = "barrel_purchase_items",
        baseColumnNames = "barrel_fee_id",
        referencedTableName = "barrel_fees",
        referencedColumnNames = "id",
        constraintName = "fk_barrel_purchase_items_fee",
      )
    }
  })
