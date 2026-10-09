// 转换自 changes/v1.0.5-add-calculated-fields.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V105AddCalculatedFields :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "1.0.5-add-calculated-fields") {
      comment("Add calculated fields for daily collection records")
      // Add calculated cash amount column
      addColumn(tableName = "daily_collection_records") {
        column(name = "calculated_cash_amount_cents", type = "bigint", defaultValue = "0") {
          constraints(nullable = false)
        }
      }
      // Add calculated QR code amount column
      addColumn(tableName = "daily_collection_records") {
        column(name = "calculated_qr_code_amount_cents", type = "bigint", defaultValue = "0") {
          constraints(nullable = false)
        }
      }
      // Add calculated water ticket count column
      addColumn(tableName = "daily_collection_records") {
        column(name = "calculated_water_ticket_count", type = "int", defaultValue = "0") {
          constraints(nullable = false)
        }
      }
      // Add calculated order amount column
      addColumn(tableName = "daily_collection_records") {
        column(name = "calculated_order_amount_cents", type = "bigint", defaultValue = "0") {
          constraints(nullable = false)
        }
      }
    }
  })
