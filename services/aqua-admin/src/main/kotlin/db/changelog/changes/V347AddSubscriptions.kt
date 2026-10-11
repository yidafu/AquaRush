// G77/F-ORD-026 周期订购计划(2026-10 新方向:后台入口创建,到期自动生成代客下单订单
// 并提醒送水工按时配送;用户端暂不开发)。计划 + 商品明细两表,次日重试的失败留痕列 last_error。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V347AddSubscriptions :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.7-add-subscriptions") {
      comment("Subscription plans + items (F-ORD-026/R-ORD-13 admin-entry auto order + worker reminder)")
      createTable(tableName = "subscriptions") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "user_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "address_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "worker_id", type = "bigint")
        column(name = "cycle_type", type = "varchar(16)") {
          constraints(nullable = false)
        }
        column(name = "weekdays", type = "varchar(64)")
        column(name = "month_day", type = "int")
        column(name = "time_slot", type = "varchar(20)") {
          constraints(nullable = false)
        }
        column(name = "start_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "end_date", type = "date")
        column(name = "max_periods", type = "int")
        column(name = "executed_periods", type = "int", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "next_run_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "status", type = "varchar(16)") {
          constraints(nullable = false)
        }
        column(name = "remark", type = "varchar(500)")
        column(name = "last_error", type = "varchar(500)")
        column(name = "last_run_at", type = "timestamp")
        column(name = "created_by", type = "bigint")
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      // 调度扫描键:ACTIVE 且到期的计划分页扫描(R-ORD-13,禁全表)
      createIndex(tableName = "subscriptions", indexName = "idx_subscription_next") {
        column(name = "status")
        column(name = "next_run_date")
      }
      createIndex(tableName = "subscriptions", indexName = "idx_subscription_user") {
        column(name = "user_id")
      }
      createTable(tableName = "subscription_items") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "subscription_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "product_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "quantity", type = "int") {
          constraints(nullable = false)
        }
        column(name = "created_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
        column(name = "updated_at", type = "timestamp", defaultValueComputed = "CURRENT_TIMESTAMP") {
          constraints(nullable = false)
        }
      }
      createIndex(tableName = "subscription_items", indexName = "idx_subscription_items_sub") {
        column(name = "subscription_id")
      }
    }
  })
