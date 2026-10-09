// 转换自 changes/v2.5.0-add-marketing-daily-facts.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V250AddMarketingDailyFacts :
  KotlinCompiledDatabaseChangeLog({
    // F-STA-007 营销统计:第 3 层日汇总中间表(夜间快照任务写入,查询侧两段合并)
    //        口径见 PRD 08-statistics.md §F-STA-007 业务规则扩充:
    //        - daily_marketing_facts:单日一行,营销订单数(含任一快照优惠,按订单去重)/券、满减、首单立减让利/用券数/水票销售与核销/赠品件数
    //        - daily_marketing_usage_facts:每日 × 营销方式 × 活动(ref_id),明细表数据源;活动名易变,查询时经 api 服务取名,不落 name 列
    changeSet(author = "aqua-rush", id = "2.5.0-add-marketing-daily-facts") {
      comment("Add atomic daily marketing fact tables for Layer-3 historical trend metrics (nightly snapshot)")
      // 营销日事实:date → 营销订单数/让利总额(券+满减+首单立减快照列)/用券数/水票销售与核销/赠品件数
      createTable(tableName = "daily_marketing_facts") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "stat_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "marketing_order_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "coupon_benefit_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "promotion_benefit_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "first_order_benefit_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "coupon_redeemed_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "ticket_sales_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "ticket_sales_revenue_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "ticket_redeemed_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "gift_quantity", type = "bigint", defaultValueNumeric = 0) {
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
        tableName = "daily_marketing_facts",
        columnNames = "stat_date",
        constraintName = "uk_daily_marketing_facts_date",
      )
      createIndex(tableName = "daily_marketing_facts", indexName = "idx_daily_marketing_facts_date") {
        column(name = "stat_date")
      }
      // 营销使用日事实:date × kind × ref_id(营销方式使用明细数据源)
      createTable(tableName = "daily_marketing_usage_facts") {
        column(name = "id", type = "bigint") {
          constraints(primaryKey = true, nullable = false)
        }
        column(name = "stat_date", type = "date") {
          constraints(nullable = false)
        }
        column(name = "kind", type = "varchar(30)") {
          constraints(nullable = false)
        }
        column(name = "ref_id", type = "bigint") {
          constraints(nullable = false)
        }
        column(name = "order_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "benefit_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "usage_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "ticket_sales_count", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "ticket_sales_revenue_cents", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
        column(name = "ticket_redeemed_count", type = "bigint", defaultValueNumeric = 0) {
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
        tableName = "daily_marketing_usage_facts",
        columnNames = "stat_date, kind, ref_id",
        constraintName = "uk_daily_marketing_usage_facts_date_kind_ref",
      )
      createIndex(
        tableName = "daily_marketing_usage_facts",
        indexName = "idx_daily_marketing_usage_facts_date_kind",
      ) {
        column(name = "stat_date")
        column(name = "kind")
      }
    }
  })
