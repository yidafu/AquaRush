// B2 订单 7 态迁移(裁决结构 #2,PRD v0.27/04-order R-ORD-11「2026-10 简化」):废「待确认收货」。
// 1) 存量终态化:AWAITING_CONFIRMATION 订单直接置 COMPLETED,completed_at 回填
//    (delivery_confirmed_at 送达时间优先,其次 updated_at)——营收/日报/对账锚 completed_at 口径即时生效;
// 2) daily_order_facts 状态分布快照同步改标(历史行重贴标签,计数求和不变);
// 3) 删系统参数 auto_confirm_receipt_days(自动确认调度已随本批移除);
// 4) 删恒等列 orders.delivery_confirmed_at(唯一业务写点 completeDelivery 已改为写 completed_at)。
// 注意:order_operations 中 ORDER_CONFIRMED/AUTO_CONFIRMED 历史留痕保留(枚举值不删)。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V343FinalizeAwaitingOrders :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.3-finalize-awaiting-orders") {
      comment("Finalize legacy AWAITING_CONFIRMATION orders to COMPLETED and drop delivery_confirmed_at (order 7-state, R-ORD-11)")
      sql(
        sql =
          "UPDATE orders SET status = 'COMPLETED', " +
            "completed_at = COALESCE(completed_at, delivery_confirmed_at, updated_at, CURRENT_TIMESTAMP) " +
            "WHERE status = 'AWAITING_CONFIRMATION'",
      )
      // 兜底:历史 COMPLETED 行若 completed_at 为空(旧种子数据曾依赖 delivery_confirmed_at 作送达锚),
      // 在删列前回填,保证「已完成按 completed_at」口径无空洞
      sql(
        sql =
          "UPDATE orders SET completed_at = COALESCE(delivery_confirmed_at, updated_at, CURRENT_TIMESTAMP) " +
            "WHERE status = 'COMPLETED' AND completed_at IS NULL",
      )
      sql(sql = "UPDATE daily_order_facts SET status = 'COMPLETED' WHERE status = 'AWAITING_CONFIRMATION'")
      sql(sql = "DELETE FROM system_settings WHERE setting_key = 'auto_confirm_receipt_days'")
      sql(sql = "ALTER TABLE orders DROP COLUMN IF EXISTS delivery_confirmed_at")
    }
  })
