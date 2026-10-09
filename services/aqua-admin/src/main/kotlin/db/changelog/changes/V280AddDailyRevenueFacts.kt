// 转换自 changes/v2.8.0-add-daily-revenue-facts.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V280AddDailyRevenueFacts :
  KotlinCompiledDatabaseChangeLog({
    // 已完成营收日事实:营收按完成时间(completed_at)归属日,与订单数(下单日)分离。
    //        daily_order_facts 的 COMPLETED 行按 created_at 归属,只用于订单数/状态分布;
    //        营收/已完成订单数改由本表提供(PRD 01-overview §日完成订单数 = 当日进入「已完成」状态的订单数)。
    changeSet(author = "aqua-rush", id = "2.8.0-add-daily-revenue-facts") {
      comment("Add daily completed-revenue fact table (revenue attributed by completed_at day)")
      createTable(tableName = "daily_revenue_facts") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "stat_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "order_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "amount_cents", type = "bigint", defaultValueNumeric = 0) {
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
        tableName = "daily_revenue_facts",
        columnNames = "stat_date",
        constraintName = "uk_daily_revenue_facts_date",
      )
      createIndex(tableName = "daily_revenue_facts", indexName = "idx_daily_revenue_facts_date") {
        column(name = "stat_date")
      }
    }
  })
