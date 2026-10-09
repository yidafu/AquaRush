// 转换自 changes/v2.0.0-add-review-columns.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V200AddReviewColumns :
  KotlinCompiledDatabaseChangeLog({
    // G23 配送照片核验闭环(orders 核验留痕列)+ G28 争议日报驳回留痕列(daily_collection_records):
    //     - orders:delivery_photo_review_status/at/by/remark —— 后台「通过/驳回」核验流留痕;
    //     - daily_collection_records:rejected_at/by/reject_reason —— 管理员驳回争议日报留痕。
    changeSet(author = "aqua-rush", id = "2.0.0-add-delivery-photo-review-columns") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "orders.delivery_photo_review_status 已存在,跳过") {
        not {
          columnExists(tableName = "orders", columnName = "delivery_photo_review_status")
        }
      }
      comment("Add delivery photo review columns to orders (G23)")
      addColumn(tableName = "orders") {
        column(name = "delivery_photo_review_status", type = "varchar(20)")
        column(name = "delivery_photo_reviewed_at", type = "timestamp")
        column(name = "delivery_photo_reviewed_by", type = "bigint")
        column(name = "delivery_photo_review_remark", type = "varchar(500)")
      }
    }
    changeSet(author = "aqua-rush", id = "2.0.0-add-daily-collection-reject-columns") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "daily_collection_records.rejected_at 已存在,跳过") {
        not {
          columnExists(tableName = "daily_collection_records", columnName = "rejected_at")
        }
      }
      comment("Add reject columns to daily_collection_records (G28)")
      addColumn(tableName = "daily_collection_records") {
        column(name = "rejected_at", type = "timestamp")
        column(name = "rejected_by", type = "bigint")
        column(name = "reject_reason", type = "varchar(500)")
      }
    }
  })
