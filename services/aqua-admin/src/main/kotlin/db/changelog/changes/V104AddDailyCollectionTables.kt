// 转换自 changes/v1.0.4-add-daily-collection-tables.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V104AddDailyCollectionTables :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.0.4-add-daily-collection-tables") {
      comment("Add daily collection and reconciliation tables for cashier reconciliation")
      // 每日收款记录表
      createTable(tableName = "aqua_daily_collection_records") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "delivery_worker_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "collection_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "cash_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "water_ticket_count", type = "int") {
          constraints(nullable = false)
        }
        column(name = "qr_code_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "order_count", type = "int")
        column(name = "order_amount_cents", type = "bigint")
        column(name = "difference_amount_cents", type = "bigint")
        column(name = "status", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "confirmed_at", type = "timestamp")
        column(name = "confirmed_by", type = "bigint")
        column(name = "notes", type = "varchar(500)")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
        // 计算列(v1.0.5,日报统计口径)
        column(name = "calculated_cash_amount_cents", type = "bigint", defaultValue = "0") {
          constraints(nullable = false)
        }
        column(name = "calculated_qr_code_amount_cents", type = "bigint", defaultValue = "0") {
          constraints(nullable = false)
        }
        column(name = "calculated_water_ticket_count", type = "int", defaultValue = "0") {
          constraints(nullable = false)
        }
        column(name = "calculated_order_amount_cents", type = "bigint", defaultValue = "0") {
          constraints(nullable = false)
        }
        // 争议日报驳回留痕(v2.0.0,G28)
        column(name = "rejected_at", type = "timestamp")
        column(name = "rejected_by", type = "bigint")
        column(name = "reject_reason", type = "varchar(500)")
      }
      // 添加唯一约束：每个配送员每天只能有一条记录
      addUniqueConstraint(
        tableName = "aqua_daily_collection_records",
        columnNames = "delivery_worker_id, collection_date",
        constraintName = "uk_daily_collection_worker_date",
      )
      // 创建索引
      createIndex(tableName = "aqua_daily_collection_records", indexName = "idx_dcr_worker_date") {
        column(name = "delivery_worker_id")
        column(name = "collection_date")
      }
      createIndex(tableName = "aqua_daily_collection_records", indexName = "idx_dcr_collection_date") {
        column(name = "collection_date")
      }
      createIndex(tableName = "aqua_daily_collection_records", indexName = "idx_dcr_status") {
        column(name = "status")
      }
      // 每日对账记录表
      createTable(tableName = "aqua_daily_reconciliations") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "reconciliation_date", type = "date") {
          constraints(nullable = false, unique = true)
        }
        column(name = "total_cash_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "total_water_ticket_count", type = "int") {
          constraints(nullable = false)
        }
        column(name = "total_qr_code_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "total_order_count", type = "int") {
          constraints(nullable = false)
        }
        column(name = "total_order_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "total_collection_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "discrepancy_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "report_data", type = "text")
        column(name = "reviewed_at", type = "timestamp")
        column(name = "reviewed_by", type = "bigint")
        column(name = "review_notes", type = "varchar(500)")
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "is_deleted", type = "boolean", defaultValue = "false") {
          constraints(nullable = false)
        }
      }
      // 创建索引
      createIndex(tableName = "aqua_daily_reconciliations", indexName = "idx_dr_reconciliation_date") {
        column(name = "reconciliation_date")
      }
      createIndex(tableName = "aqua_daily_reconciliations", indexName = "idx_dr_status") {
        column(name = "status")
      }
    }
  })
