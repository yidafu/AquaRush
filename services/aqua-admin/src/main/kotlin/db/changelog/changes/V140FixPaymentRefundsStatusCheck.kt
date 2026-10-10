// 转换自 changes/v1.4.0-fix-payment-refunds-status-check.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V140FixPaymentRefundsStatusCheck :
  KotlinCompiledDatabaseChangeLog({
    // payment_refunds.status CHECK 对齐:
    //     旧约束 (PENDING/SUCCESS/FAILED/CANCELLED) 与生成枚举 RefundStatus
    //     (PENDING/APPROVED/REJECTED/COMPLETED/FAILED) 及 PRD 04-order.md §R-PAY-02
    //     状态机(待审核/已通过/已拒绝/已完成/失败)不一致,导致退款记录无法落库。
    //     开发期不兼容修改(AGENTS.md 规则 9),仅影响开发/测试库(master changelog 为重建式)。
    changeSet(author = "aqua-rush", id = "1.4.0-fix-payment-refunds-status-check") {
      comment("Align payment_refunds.status CHECK with RefundStatus enum (R-PAY-02)")
      sql(
        sql = """
      ALTER TABLE payment_refunds DROP CONSTRAINT chk_payment_refunds_status;
    
            """,
      )
      sql(
        sql = """
      ALTER TABLE payment_refunds ADD CONSTRAINT chk_payment_refunds_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'FAILED'));
    
            """,
      )
    }
  })
