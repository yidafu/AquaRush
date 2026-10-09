// 转换自 changes/v2.0.1-add-refund-review-columns.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V201AddRefundReviewColumns :
  KotlinCompiledDatabaseChangeLog({
    // R-PAY-02 管理员退款审核流(payment_refunds 审核列 + NOT NULL 放宽):
    //     - refund_id/refund_no 原为 NOT NULL,退款申请(PENDING)阶段尚未执行微信退款无单号,须放宽;
    //     - refund_type:订单退款/押金退还口径(营收统计仅聚合 ORDER_REFUND,押金退还不计入);
    //     - admin_note:管理员审核备注;processed_by:审核/处理人;
    //     - failure_reason 已由 v1.0.0 基础 schema 创建(payment_refunds.failure_reason TEXT),此处不再重复添加。
    changeSet(author = "aqua-rush", id = "2.0.1-relax-refund-id-not-null") {
      comment("Relax payment_refunds.refund_id/refund_no to nullable for PENDING refund requests (R-PAY-02)")
      sql(
        sql = """
      ALTER TABLE payment_refunds ALTER COLUMN refund_id DROP NOT NULL;
      ALTER TABLE payment_refunds ALTER COLUMN refund_no DROP NOT NULL;
    
            """,
      )
    }
    changeSet(author = "aqua-rush", id = "2.0.1-add-refund-review-columns") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "payment_refunds.refund_type 已存在,跳过") {
        not {
          columnExists(tableName = "payment_refunds", columnName = "refund_type")
        }
      }
      comment(
        "Add refund_type/admin_note/processed_by columns to payment_refunds (R-PAY-02/G06); failure_reason already exists in base schema",
      )
      addColumn(tableName = "payment_refunds") {
        column(name = "refund_type", type = "varchar(20)", defaultValue = "ORDER_REFUND") {
          constraints(nullable = false)
        }
        column(name = "admin_note", type = "varchar(1000)")
        column(name = "processed_by", type = "bigint")
      }
    }
  })
