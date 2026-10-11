// 转换自 changes/v2.0.2-add-deposit.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V202AddDeposit :
  KotlinCompiledDatabaseChangeLog({
    // G06 桶押金模块(新增水桶押金模块):
    //     - aqua_orders.deposit_amount_cents:订单押金合计快照(随单一并收取,R-MON-03);
    //     - aqua_deposit_records:押金记录表(用户/金额/状态/收取与退还时间,收取→退还闭环)。
    changeSet(author = "aqua-rush", id = "2.0.2-add-order-deposit-column") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "aqua_orders.deposit_amount_cents 已存在,跳过") {
        not {
          columnExists(tableName = "aqua_orders", columnName = "deposit_amount_cents")
        }
      }
      comment("Add deposit_amount_cents snapshot column to aqua_orders (G06/R-MON-03)")
      addColumn(tableName = "aqua_orders") {
        column(name = "deposit_amount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
      }
    }
    changeSet(author = "aqua-rush", id = "2.0.2-create-deposit-records-table") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "aqua_deposit_records 表已存在,跳过") {
        not {
          tableExists(tableName = "aqua_deposit_records")
        }
      }
      comment("Create aqua_deposit_records table (G06)")
      createTable(tableName = "aqua_deposit_records") {
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
        // 押金桶规格口径扩列(v3.4.6,R-MON-03:在租桶数按规格统计)
        column(name = "specification", type = "varchar(100)")
        column(name = "unit_deposit_cents", type = "bigint")
        column(name = "bucket_count", type = "int")
      }
      createIndex(tableName = "aqua_deposit_records", indexName = "idx_deposit_records_order_id") {
        column(name = "order_id")
      }
      createIndex(tableName = "aqua_deposit_records", indexName = "idx_deposit_records_user_id") {
        column(name = "user_id")
      }
      createIndex(tableName = "aqua_deposit_records", indexName = "idx_deposit_records_status") {
        column(name = "status")
      }
      createIndex(tableName = "aqua_deposit_records", indexName = "idx_deposit_records_collected_at") {
        column(name = "collected_at")
      }
      addForeignKeyConstraint(
        baseTableName = "aqua_deposit_records",
        baseColumnNames = "order_id",
        referencedTableName = "aqua_orders",
        referencedColumnNames = "id",
        constraintName = "fk_deposit_records_order",
      )
      addForeignKeyConstraint(
        baseTableName = "aqua_deposit_records",
        baseColumnNames = "user_id",
        referencedTableName = "aqua_users",
        referencedColumnNames = "id",
        constraintName = "fk_deposit_records_user",
      )
    }
  })
