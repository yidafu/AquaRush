// 转换自 changes/v2.7.0-add-payment-expiry-optimization.xml (liquibase-kotlin 4.33.0-0.10.1)
// changeSet id/author 逐字保留；本文件为编译期 changelog，路径 = 类 FQCN。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V270AddPaymentExpiryOptimization :
  KotlinCompiledDatabaseChangeLog({
    // R-ORD-04 支付超时优化(MQ 延迟消息主触发 + 定时扫表兜底 + 数据库乐观锁):
    //     - payments.version:乐观锁版本列,支付超时 CAS 认领(PENDING→EXPIRED, version+1)
    //       与回调/退款/重新支付等并发写方的互斥与幂等基础;
    //     - chk_payments_status 重建:旧约束 (PENDING/PAID/FAILED/CANCELLED/REFUNDED)
    //       与域枚举 PaymentStatus(PENDING/PROCESSING/SUCCESS/FAILED/CANCELLED/EXPIRED/
    //       REFUNDING/REFUNDED)不一致,现有代码已写入 SUCCESS/EXPIRED 会被旧 CHECK 拦截
    //       (v1.4.0 仅修复了 payment_refunds,payments 存量问题一并对齐)。
    changeSet(author = "aqua-rush", id = "2.7.0-add-payments-version") {
      preConditions(onFail = "CONTINUE", onErrorMessage = "payments.version 已存在,跳过") {
        not {
          columnExists(tableName = "payments", columnName = "version")
        }
      }
      comment("Add optimistic lock version column to payments (R-ORD-04)")
      addColumn(tableName = "payments") {
        column(name = "version", type = "bigint", defaultValueNumeric = 0) {
          constraints(nullable = false)
        }
      }
    }
    changeSet(author = "aqua-rush", id = "2.7.0-fix-payments-status-check") {
      comment("Align payments.status CHECK with PaymentStatus enum (R-ORD-04)")
      sql(
        sql = """
      ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payments_status;
    
            """,
      )
      sql(
        sql = """
      ALTER TABLE payments ADD CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED',
        'EXPIRED', 'REFUNDING', 'REFUNDED'));
    
            """,
      )
    }
  })
