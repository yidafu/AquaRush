// B2 缺陷 #9(裁决第一节):派单/抢单记时补列。
// orders.assigned_at = 派单或抢单成功时刻(目标态 03 §6:状态谓词更新语句同步写入),
// 履约时长起点由「order_operations 最早 DELIVERY_ASSIGNED」迁移为本列权威读取。
// 存量回填:取各订单最早 DELIVERY_ASSIGNED 操作时间(管理员派单与送水工接单均记录该操作)。
package db.changelog.changes

import momosetkn.liquibase.kotlin.parser.KotlinCompiledDatabaseChangeLog

class V344AddOrdersAssignedAt :
  KotlinCompiledDatabaseChangeLog({
    changeSet(author = "aqua-rush", id = "3.4.4-add-orders-assigned-at") {
      comment("Add orders.assigned_at (defect #9 dispatch/grab timestamp) and backfill from DELIVERY_ASSIGNED operations")
      sql(sql = "ALTER TABLE orders ADD COLUMN IF NOT EXISTS assigned_at timestamp")
      sql(
        sql =
          "UPDATE orders o SET assigned_at = sub.first_assigned FROM (" +
            "SELECT order_id, MIN(created_at) AS first_assigned FROM order_operations " +
            "WHERE operation_type = 'DELIVERY_ASSIGNED' GROUP BY order_id" +
            ") sub WHERE o.id = sub.order_id AND o.assigned_at IS NULL",
      )
    }
  })
