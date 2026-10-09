// 转换自 changes/v2.0.2-add-deposit.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V202AddDeposit :
  KotlinCompiledDatabaseChangeLog({
    // G06 桶押金模块(新增水桶押金模块):
    //     - orders.deposit_amount_cents:订单押金合计快照(随单一并收取,R-MON-03);
    //     - deposit_records:押金记录表(用户/金额/状态/收取与退还时间,收取→退还闭环)。
    changeSet(author = "aqua-rush", id = "2.0.2-add-order-deposit-column") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "orders.deposit_amount_cents 已存在,跳过") {
        not {
          columnExists(tableName = "orders", columnName = "deposit_amount_cents")
        }
      }
      comment("Add deposit_amount_cents snapshot column to orders (G06/R-MON-03)")
      addColumn(tableName = "orders") {
        column(name = "deposit_amount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
      }
    }
    changeSet(author = "aqua-rush", id = "2.0.2-create-deposit-records-table") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "deposit_records 表已存在,跳过") {
        not {
          tableExists(tableName = "deposit_records")
        }
      }
      comment("Create deposit_records table (G06)")
      createTable(tableName = "deposit_records") {
        column(name = "id", type = "bigint", autoIncrement = true) {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "order_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "deposit_amount_cents", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(20)", defaultValue = "COLLECTED") {
          constraints(nullable = false)
        }
        column(name = "collected_at", type = "timestamp")
        column(name = "refunded_at", type = "timestamp")
        column(name = "refunded_by", type = "bigint")
        column(name = "refund_remark", type = "varchar(500)")
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
      createIndex(tableName = "deposit_records", indexName = "idx_deposit_records_order_id") {
        column(name = "order_id")
      }
      createIndex(tableName = "deposit_records", indexName = "idx_deposit_records_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "deposit_records", indexName = "idx_deposit_records_status") {
        column(name = "status")
      }
      createIndex(tableName = "deposit_records", indexName = "idx_deposit_records_collected_at") {
        column(name = "collected_at")
      }
      addForeignKeyConstraint(
        baseTableName = "deposit_records",
        baseColumnNames = "order_id",
        referencedTableName = "orders",
        referencedColumnNames = "id",
        constraintName = "fk_deposit_records_order",
      )
      addForeignKeyConstraint(
        baseTableName = "deposit_records",
        baseColumnNames = "user_id",
        referencedTableName = "users",
        referencedColumnNames = "id",
        constraintName = "fk_deposit_records_user",
      )
    }
  })
