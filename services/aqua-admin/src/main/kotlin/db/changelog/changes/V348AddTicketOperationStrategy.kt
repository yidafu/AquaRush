// G97 水票运营策略(PRD 10-marketing.md §R-MKT-08 运营策略行/F-MKT-007 扩充,依调研报告 v2 B1/S4):
// ticket_types 增三列运营策略开关(引流票/复购激活单数/单次核销上限,票种级可配、默认不启用);
// ticket_purchases 增复购激活基线列(购票时刻已完成订单数,启用激活的票种购票时落值)。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V348AddTicketOperationStrategy :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.8-add-ticket-operation-strategy") {
      comment(
        "G97 water ticket operation strategy: lead-generation flag / activation order count / max redeem per order on ticket_types, activation baseline on ticket_purchases",
      )
      sql(sql = "ALTER TABLE ticket_types ADD COLUMN IF NOT EXISTS is_lead_generation boolean NOT NULL DEFAULT false")
      sql(sql = "ALTER TABLE ticket_types ADD COLUMN IF NOT EXISTS activation_order_count integer")
      sql(sql = "ALTER TABLE ticket_types ADD COLUMN IF NOT EXISTS max_redeem_per_order integer")
      sql(sql = "ALTER TABLE ticket_purchases ADD COLUMN IF NOT EXISTS completed_orders_at_purchase integer")
    }
  })
