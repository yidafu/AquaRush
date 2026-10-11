// 转换自 changes/v2.4.0-add-daily-stat-facts.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V240AddDailyStatFacts :
  KotlinCompiledDatabaseChangeLog({
    // 第 3 层历史趋势指标:原子日汇总中间表(夜间快照任务写入,查询侧两段合并)
    changeSet(author = "aqua-rush", id = "2.4.0-add-daily-stat-facts") {
      comment("Add atomic daily summary fact tables for Layer-3 historical trend metrics (nightly snapshot)")
      // 订单日事实:date × status × delivery_worker_id(amount 按单一次,不乘件数 P1-3;item_quantity 按明细)
      createTable(tableName = "aqua_daily_order_facts") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "stat_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "delivery_worker_id", type = "bigint")
        column(name = "order_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "amount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "item_quantity", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
      }
      addUniqueConstraint(
        tableName = "aqua_daily_order_facts",
        columnNames = "stat_date, status, delivery_worker_id",
        constraintName = "uk_daily_order_facts_date_status_worker",
      )
      createIndex(tableName = "aqua_daily_order_facts", indexName = "idx_daily_order_facts_date") {
        column(name = "stat_date")
      }
      // 商品日事实:date × status × product_id(商品名易变,查询时 join 商品表取名)
      createTable(tableName = "aqua_daily_product_facts") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "stat_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "quantity", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "revenue_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "order_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
      }
      addUniqueConstraint(
        tableName = "aqua_daily_product_facts",
        columnNames = "stat_date, status, product_id",
        constraintName = "uk_daily_product_facts_date_status_product",
      )
      createIndex(tableName = "aqua_daily_product_facts", indexName = "idx_daily_product_facts_date") {
        column(name = "stat_date")
      }
      // 用户日事实:date → 新增用户数 / 登录用户数(USER_LOGIN 按 userId 去重)
      createTable(tableName = "aqua_daily_user_facts") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "stat_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "new_user_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "login_user_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
      }
      addUniqueConstraint(
        tableName = "aqua_daily_user_facts",
        columnNames = "stat_date",
        constraintName = "uk_daily_user_facts_date",
      )
      createIndex(tableName = "aqua_daily_user_facts", indexName = "idx_daily_user_facts_date") {
        column(name = "stat_date")
      }
      // 退款日事实:date(processed_at 归属日) × status × refund_type(查询时组合:COMPLETED + ORDER_REFUND)
      createTable(tableName = "aqua_daily_refund_facts") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "stat_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "refund_type", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "refund_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "refund_amount_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp") {
          constraints(nullable = false)
        }
      }
      addUniqueConstraint(
        tableName = "aqua_daily_refund_facts",
        columnNames = "stat_date, status, refund_type",
        constraintName = "uk_daily_refund_facts_date_status_type",
      )
      createIndex(tableName = "aqua_daily_refund_facts", indexName = "idx_daily_refund_facts_date") {
        column(name = "stat_date")
      }
    }
  })
